# Serviço de Geração e Envio de Mensagem PIX (pacs.008)

![Java](https://img.shields.io/badge/Java-21-blue.svg)![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg)![JMS](https://img.shields.io/badge/JMS-3.1-orange.svg)![IBM MQ](https://img.shields.io/badge/IBM%20MQ-Client-blue.svg)![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)

Este projeto é uma implementação de um serviço para gerar e **enviar** mensagens `pacs.008` (Ordem de Pagamento) do Sistema de Pagamentos Instantâneos (PIX) para um servidor de mensageria real, com base nas especificações do **Catálogo de Serviços do SFN - Volume VI**.

**ATENÇÃO:** Esta implementação é um **template de produção**. Ela utiliza as APIs e configurações corretas para se conectar a um ambiente real do SPI, mas requer que você forneça as credenciais, certificados e endereços de rede fornecidos pelo Banco Central.

A aplicação consiste em:
-   **Backend:** Um serviço RESTful com **Java 21** e **Spring Boot** que gera a mensagem XML `pacs.008` e a envia para um servidor **IBM MQ** utilizando o padrão **JMS (Java Message Service)** sobre uma conexão segura **mTLS**.
-   **Frontend:** Uma interface web para iniciar o fluxo, permitindo ao usuário gerar a mensagem e disparar o envio para o servidor MQ.
-   **Containerização:** O projeto é totalmente containerizado com **Docker** para portabilidade e facilidade de deploy.

## Arquitetura de Conexão Real com o SPI

Em um ambiente de produção, após a geração e assinatura digital, a mensagem `pacs.008` é enviada para a **Rede do Sistema Financeiro Nacional (RSFN)** e publicada em uma fila de mensagens segura (IBM MQ) para que o SPI a processe.

Esta implementação utiliza:
-   `spring-boot-starter-jms`: Para abstrair a complexidade da comunicação com filas.
-   `com.ibm.mq.allclient`: O driver oficial para comunicação com servidores IBM MQ.
-   `JmsConfig.java`: Uma classe de configuração que estabelece a conexão segura (mTLS) utilizando um **keystore** (com o certificado do cliente) e um **truststore** (com os certificados da autoridade do SPI).

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

## Pré-requisitos para Instalação

-   [Git](https://git-scm.com/)
-   [Docker](https://www.docker.com/products/docker-desktop/)
-   [Docker Compose](https://docs.docker.com/compose/install/) (geralmente incluído no Docker Desktop)

## Como Configurar e Instalar

### 1. Obtenha seus Certificados e Credenciais
Antes de executar, você precisa:
-   Gerar ou obter seus arquivos `keystore.jks` e `truststore.jks` conforme as especificações do Banco Central.
-   Colocar esses arquivos no diretório `src/main/resources/certs/`.
-   Obter os dados de conexão do seu ambiente de homologação/produção do SPI (host, porta, queue manager, canal, usuário, senhas, etc.).

### 2. Configure a Conexão
Abra o arquivo `src/main/resources/application.properties` e **substitua todos os valores de placeholder** (`SEU_...`, `SENHA_...`, etc.) pelas credenciais reais que você obteve.

**⚠️ AVISO DE SEGURANÇA:** Nunca comite senhas ou segredos diretamente no controle de versão. Para ambientes de produção, utilize um cofre de segredos (como HashiCorp Vault, AWS/GCP/Azure Secrets Manager) e injete as senhas como variáveis de ambiente no seu container.

### 3. Instale e Execute com Docker

1.  **Clone o repositório:**
    ```bash
    git clone <URL_DO_SEU_REPOSITORIO> pixspi
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
3.  **Passo 1:** Clique no botão **"Gerar Mensagem"**. O XML correspondente será exibido, e o botão de publicação será habilitado.
4.  **Passo 2:** Clique no botão **"Publicar Mensagem no SPI"**.
5.  O serviço tentará estabelecer uma conexão real com o servidor MQ configurado no `application.properties`.
6.  Verifique o **status da publicação** na interface e os **logs detalhados** no console do terminal onde o Docker está rodando para diagnosticar o sucesso ou falha da conexão.

---

> ### **Nota Importante sobre a Assinatura Digital**
>
> ⚠️ A funcionalidade de assinatura digital (`SignatureService.java`) ainda é um **stub** (simulação). Para um ambiente de produção, você deve substituir o código simulado por uma implementação robusta utilizando a API Java XML Digital Signature (XMLDSig) e o certificado contido no seu `keystore.jks`.