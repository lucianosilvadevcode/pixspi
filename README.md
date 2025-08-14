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

### Descrição dos Arquivos do Projeto

-   **`.gitignore`**: Define quais arquivos e diretórios devem ser ignorados pelo sistema de controle de versão Git (ex: arquivos de build, logs, configurações de IDE).
-   **`Dockerfile`**: Contém as instruções para construir a imagem Docker da aplicação, empacotando o ambiente de execução e a aplicação em um container portátil.
-   **`docker-compose.yml`**: Arquivo de configuração que simplifica a execução do container Docker, definindo o serviço, o mapeamento de portas e outras configurações de tempo de execução.
-   **`pom.xml`**: O "Project Object Model" do Maven. Gerencia as dependências do projeto (bibliotecas), plugins e o processo de build da aplicação Java.

-   **`src/main/java`**: Contém todo o código-fonte do backend em Java.
    -   **`Pacs008Application.java`**: O ponto de entrada principal que inicializa a aplicação Spring Boot.
    -   **`config/JmsConfig.java`**: Classe de configuração que cria e configura a conexão segura (mTLS) com o servidor IBM MQ, utilizando as credenciais e certificados definidos.
    -   **`controller/Pacs008Controller.java`**: Expõe os endpoints da API REST (`/payments` e `/publish`) que a interface do usuário consome para gerar e enviar as mensagens.
    -   **`dto/PaymentRequestDto.java`**: Objeto de Transferência de Dados (DTO) que mapeia os dados JSON recebidos do formulário de pagamento.
    -   **`dto/PublishRequestDto.java`**: DTO que encapsula o conteúdo XML para ser enviado ao endpoint de publicação.
    -   **`service/mq/MqClientService.java`**: Serviço que implementa a lógica de envio da mensagem para a fila do IBM MQ, utilizando o `JmsTemplate` do Spring.
    -   **`service/Pacs008Service.java`**: Contém a lógica de negócio principal para construir o objeto da mensagem `pacs.008` a partir dos dados recebidos.
    -   **`service/SignatureService.java`**: Serviço responsável por realizar a assinatura digital criptográfica no padrão `XMLDSig` sobre a mensagem XML gerada.

-   **`src/main/resources`**: Contém todos os arquivos de configuração e recursos estáticos.
    -   **`certs/keystore.jks`**: Arquivo Java KeyStore que armazena a chave privada e o certificado público da sua instituição, usado para provar sua identidade.
    -   **`certs/truststore.jks`**: Arquivo Java KeyStore que armazena os certificados públicos das Autoridades Certificadoras confiáveis (do Banco Central), usado para verificar a identidade do servidor do SPI.
    -   **`static/`**: Diretório para os arquivos do frontend.
        -   **`index.html`**: A estrutura da página web que o usuário acessa.
        -   **`script.js`**: A lógica JavaScript para capturar os dados do formulário, chamar a API do backend e exibir os resultados.
        -   **`style.css`**: As regras de estilo para a aparência da página web.
    -   **`xsd/pacs.008.spi.1.13.xsd`**: O arquivo de Schema XML oficial do `pacs.008`. É usado pelo plugin JAXB para gerar as classes Java correspondentes automaticamente.
    -   **`application.properties`**: Arquivo de configuração principal do Spring Boot. Contém as configurações do servidor, os detalhes de conexão com o MQ e as senhas e alias dos keystores.


## Configurando a Segurança (mTLS): Gerando os Arquivos `.jks`

Os arquivos `keystore.jks` e `truststore.jks` são os pilares da segurança da conexão. Eles **não são gerados pela aplicação**, mas sim através de um processo formal de certificação digital. Você precisará da ferramenta `keytool`, que é incluída em qualquer instalação do Java JDK.

---
### **I - O `keystore.jks` (Sua Identidade)**
Contém sua chave privada e seu certificado público. É usado para provar sua identidade ao servidor do SPI.

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
3.  Após a validação, a CA lhe devolverá seu **certificado público assinado** (ex: `seu_psp.cer`) e os **certificados da própria CA**.

#### Passo 4: Importar os Certificados para o `keystore.jks`
Importe a cadeia de certificados da CA e, por último, o seu próprio certificado assinado para dentro do `keystore.jks`.
```bash
keytool -importcert -alias ca_raiz -file ca_raiz.cer -keystore keystore.jks
keytool -importcert -alias ca_intermediaria -file ca_intermediaria.cer -keystore keystore.jks
keytool -importcert -alias seu-psp-alias -file seu_psp.cer -keystore keystore.jks
```

---
### **II - O `truststore.jks` (Sua Lista de Confiança)**
Contém os certificados públicos da Autoridade Certificadora do Banco Central. É usado para verificar a identidade do servidor do SPI. **Os arquivos `.cer` para este processo são fornecidos pelo Banco Central.**

#### Passo 1: Criar o `truststore.jks` importando a CA Raiz
Este comando cria o arquivo `truststore.jks` e adiciona o primeiro certificado da cadeia de confiança.

```bash
keytool -importcert -alias ca_raiz_sfn -file AC_Raiz_SFN.cer -keystore truststore.jks
```
-   Responda **"sim"** à pergunta "Confiar neste certificado?".
-   Você será solicitado a criar uma senha para o truststore. **Guarde-a com segurança**, pois ela será a `ibm.mq.ssl.trust-store-password`.

#### Passo 2: Importar os demais Certificados da Cadeia
Adicione os certificados intermediários fornecidos pelo BCB ao mesmo arquivo.

```bash
keytool -importcert -alias ca_intermediaria_spi -file AC_Intermediaria_SPI_v1.cer -keystore truststore.jks
```

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
    git clone https://github.com/lucianosilvadevcode/pixspi.git
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
