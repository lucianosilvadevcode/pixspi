# Serviço de Geração e Envio de Mensagem PIX (pacs.008)

![Java](https://img.shields.io/badge/Java-21-blue.svg)![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg)![JMS](https://img.shields.io/badge/JMS-3.1-orange.svg)![IBM MQ](https://img.shields.io/badge/IBM%20MQ-Client-blue.svg)![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)

Este projeto é uma implementação de um serviço para gerar, **assinar digitalmente** e **enviar** mensagens `pacs.008` (Ordem de Pagamento) do Sistema de Pagamentos Instantâneos (PIX) para um servidor de mensageria real, com base nas especificações do **Catálogo de Serviços do SFN - Volume VI**.

**ATENÇÃO:** Esta implementação é um **template de produção**. Ela utiliza as APIs e configurações corretas para se conectar a um ambiente real do SPI, mas requer que você forneça as credenciais, certificados e endereços de rede fornecidos pelo Banco Central.

A aplicação consiste em:
-   **Backend:** Um serviço RESTful com **Java 21** e **Spring Boot** que gera a mensagem XML `pacs.008`, a **assina criptograficamente** com o padrão `XMLDSig`, e a envia para um servidor **IBM MQ** utilizando o padrão **JMS (Java Message Service)** sobre uma conexão segura **mTLS**.
-   **Frontend:** Uma interface web para iniciar o fluxo, permitindo ao usuário gerar a mensagem e disparar o envio para o servidor MQ.
-   **Containerização:** O projeto é totalmente containerizado com **Docker** para portabilidade e facilidade de deploy.

## Arquitetura de Conexão e Segurança

### 1. Assinatura Digital (XMLDSig)
A autenticidade e a integridade de cada mensagem enviada ao SPI são garantidas por uma assinatura digital. O `SignatureService.java` implementa esta funcionalidade utilizando a API Java XML Digital Signature. Ele executa uma assinatura do tipo "Enveloped", onde a tag `<Signature>` é inserida dentro do próprio XML, e utiliza a chave privada contida no `keystore.jks` para realizar a operação criptográfica.

### 2. Conexão Segura (mTLS) e Envio (JMS)
Após a assinatura, a mensagem é enviada para a **Rede do Sistema Financeiro Nacional (RSFN)** e publicada em uma fila de mensagens segura (IBM MQ). Esta implementação utiliza:
-   `spring-boot-starter-jms`: Para abstrair a complexidade da comunicação com filas.
-   `com.ibm.mq.allclient`: O driver oficial para comunicação com servidores IBM MQ.
-   `JmsConfig.java`: Uma classe que estabelece a conexão segura (mTLS) utilizando um **keystore** (com o certificado do cliente) e um **truststore** (com os certificados da autoridade do SPI).

## Estrutura de Diretórios do Projeto

```
pixspi/
├── .gitignore
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── pacs008/
        │               ├── Pacs008Application.java
        │               ├── config/
        │               │   └── JmsConfig.java
        │               ├── controller/
        │               │   └── Pacs008Controller.java
        │               ├── dto/
        │               │   ├── PaymentRequestDto.java
        │               │   └── PublishRequestDto.java
        │               └── service/
        │                   ├── mq/
        │                   │   └── MqClientService.java
        │                   ├── Pacs008Service.java
        │                   └── SignatureService.java
        └── resources/
            ├── certs/
            │   ├── keystore.jks
            │   └── truststore.jks
            ├── static/
            │   ├── index.html
            │   ├── script.js
            │   └── style.css
            ├── xsd/
            │   └── pacs.008.spi.1.13.xsd
            └── application.properties
```

## Configurando a Segurança (mTLS): Gerando os Arquivos `.jks`

Os arquivos `keystore.jks` e `truststore.jks` são os pilares da segurança da conexão. Eles **não são gerados pela aplicação**, mas sim através de um processo formal de certificação digital. Eles funcionam como a identidade digital da sua instituição.

-   **`keystore.jks` (Seu Passaporte Digital):** Contém sua chave privada e seu certificado público. É usado para provar sua identidade ao servidor do SPI.
-   **`truststore.jks` (Sua Lista de Contatos Confiáveis):** Contém os certificados públicos da Autoridade Certificadora (CA) do Banco Central. É usado para verificar a identidade do servidor do SPI.

Você precisará da ferramenta `keytool`, que é incluída em qualquer instalação do Java JDK.

---
### **Processo de Geração**

#### Passo 1: Gerar o Par de Chaves e o `keystore.jks`
Este comando cria seu cofre inicial (`keystore.jks`) contendo um par de chaves (pública/privada) para sua instituição.

```bash
keytool -genkeypair -alias seu-psp-alias -keyalg RSA -keysize 2048 \
        -keystore keystore.jks \
        -dname "CN=psp.exemplo.com.br, OU=Tecnologia, O=Meu PSP Financeira S.A., L=Sao Paulo, ST=SP, C=BR"
```
-   **`-alias`**: Substitua `seu-psp-alias` por um identificador único para sua chave.
-   **`-dname`**: Substitua pelos dados **exatos e oficiais** da sua instituição.
-   Você será solicitado a criar uma senha para o keystore. **Guarde-a com segurança**, pois ela será usada na configuração `ibm.mq.ssl.key-store-password`.

#### Passo 2: Gerar o Pedido de Assinatura de Certificado (CSR)
Este comando extrai sua chave pública e seus dados para um arquivo de solicitação (`.csr`) que será enviado para validação.

```bash
keytool -certreq -alias seu-psp-alias -file SEU_PSP.csr -keystore keystore.jks
```

#### Passo 3: Enviar o CSR a uma Autoridade Certificadora (CA)
Este é um processo formal e externo:
1.  Envie o arquivo `SEU_PSP.csr` para a Autoridade Certificadora homologada pelo Banco Central.
2.  A CA validará a identidade da sua empresa.
3.  Após a validação, a CA lhe devolverá seu **certificado público assinado** (ex: `seu_psp.cer`) e os **certificados da própria CA** (a cadeia de confiança).

#### Passo 4: Importar os Certificados para o `keystore.jks`
Agora, você importa os certificados recebidos para dentro do seu keystore, tornando-o oficialmente válido. Importe primeiro a cadeia da CA e, por último, o seu certificado.

```bash
# Exemplo de importação da cadeia da CA
keytool -importcert -alias ca_raiz -file ca_raiz.cer -keystore keystore.jks
keytool -importcert -alias ca_intermediaria -file ca_intermediaria.cer -keystore keystore.jks

# Importação do seu certificado assinado
keytool -importcert -alias seu-psp-alias -file seu_psp.cer -keystore keystore.jks
```

#### Passo 5: Criar o `truststore.jks`
Este arquivo conterá apenas os certificados públicos da CA, que garantem a confiança no servidor do SPI.

```bash
keytool -importcert -alias ca_raiz_spi -file ca_raiz.cer -keystore truststore.jks
keytool -importcert -alias ca_intermediaria_spi -file ca_intermediaria.cer -keystore truststore.jks
```
-   Você será solicitado a criar uma senha para o truststore. **Guarde-a com segurança**, pois ela será a `ibm.mq.ssl.trust-store-password`.

---

## Como Configurar e Instalar

### 1. Gere e Posicione seus Certificados
Siga o processo descrito na seção anterior para gerar seus arquivos `keystore.jks` e `truststore.jks`. Em seguida, coloque-os no diretório `src/main/resources/certs/`.

### 2. Configure a Conexão e a Assinatura
Abra o arquivo `src/main/resources/application.properties` e **substitua todos os valores de placeholder** pelas credenciais reais que você obteve, incluindo os campos `ibm.mq.ssl.key-alias` e `ibm.mq.ssl.key-password`.

**⚠️ AVISO DE SEGURANÇA:** Nunca comite senhas ou segredos diretamente no controle de versão. Para ambientes de produção, utilize um cofre de segredos (como HashiCorp Vault) e injete as senhas como variáveis de ambiente.

### 3. Instale e Execute com Docker

1.  **Clone o repositório:**
    ```bash
    git clone https://github.com/lucianosilvadevcode/pixspi.git pixspi
    cd pixspi
    ```

2.  **Construa a imagem e inicie o container:**
    ```bash
    docker-compose up --build
    ```

3.  **Acesse a aplicação:**
    A aplicação estará disponível em **[http://localhost:8080](http://localhost:8080)**.

## Como Testar a Aplicação

1.  Acesse **[http://localhost:8080](http://localhost:8080)**.
2.  Preencha o formulário para gerar uma mensagem de teste.
3.  **Passo 1:** Clique no botão **"Gerar Mensagem"**. O XML correspondente, agora com uma tag `<Signature>` completa e criptograficamente válida, será exibido.
4.  **Passo 2:** Clique no botão **"Publicar Mensagem no SPI"**.
5.  O serviço tentará estabelecer uma conexão real com o servidor MQ configurado no `application.properties`, usando os certificados do keystore e truststore.
6.  Verifique o **status da publicação** na interface e os **logs detalhados** no console do terminal onde o Docker está rodando para diagnosticar o sucesso ou falha da conexão.