# Serviço de Geração e Envio de Mensagem PIX (pacs.008)

![Java](https://img.shields.io/badge/Java-21-blue.svg)![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg)![JMS](https://img.shields.io/badge/JMS-3.1-orange.svg)![IBM MQ](https://img.shields.io/badge/IBM%20MQ-Client-blue.svg)![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)

Este projeto é uma implementação de um serviço para gerar, **assinar digitalmente** e **enviar** mensagens `pacs.008` (Ordem de Pagamento) do Sistema de Pagamentos Instantâneos (PIX) para um servidor de mensageria real.

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
*(Seção inalterada)*

## Como Configurar e Instalar

### 1. Gere e Posicione seus Certificados
Siga o processo descrito na seção anterior para gerar seus arquivos `keystore.jks` e `truststore.jks`. Em seguida, coloque-os no diretório `src/main/resources/certs/`.

### 2. Configure a Conexão e a Assinatura
Abra o arquivo `src/main/resources/application.properties` e **substitua todos os valores de placeholder** pelas credenciais reais que você obteve, incluindo os novos campos `ibm.mq.ssl.key-alias` e `ibm.mq.ssl.key-password`.

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
3.  **Passo 1:** Clique no botão **"Gerar Mensagem"**. O XML correspondente, agora com uma tag `<Signature>` completa, será exibido.
4.  **Passo 2:** Clique no botão **"Publicar Mensagem no SPI"**.
5.  O serviço tentará estabelecer uma conexão real com o servidor MQ configurado.
6.  Verifique o **status da publicação** na interface e os **logs detalhados** no terminal para diagnosticar o sucesso ou falha da conexão.