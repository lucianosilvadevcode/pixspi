
# Gerador de Mensagem PIX (pacs.008)

![Java](https://img.shields.io/badge/Java-21-blue.svg)![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg)![Maven](https://img.shields.io/badge/Maven-3.9-red.svg)![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)

Este projeto é uma implementação de um serviço para gerar mensagens `pacs.008` (Ordem de Pagamento) do Sistema de Pagamentos Instantâneos (PIX) do Brasil, com base nas especificações do **Catálogo de Serviços do SFN - Volume VI**.

A aplicação consiste em:
-   **Backend:** Um serviço RESTful construído com **Java 21** e **Spring Boot**, que recebe dados de pagamento em formato JSON e gera a mensagem XML `pacs.008` correspondente, seguindo o XSD oficial.
-   **Frontend:** Uma página web interativa (HTML, CSS, JavaScript) que consome o serviço do backend, permitindo que o usuário insira os dados e visualize o XML gerado.
-   **Containerização:** O projeto é totalmente containerizado com **Docker** e **Docker Compose** para garantir um setup de desenvolvimento rápido, fácil e consistente.

## Especificação do Serviço `pacs.008`

A mensagem `pacs.008` (FI to FI Customer Credit Transfer) é o pilar para a iniciação de uma transferência de crédito no PIX. Ela é enviada pelo Prestador de Serviços de Pagamento (PSP) do pagador em direção ao SPI, que a repassa ao PSP do recebedor para efetivar a transação.

### Campos Chave da Mensagem `pacs.008`

| Campo XML | Caminho (simplificado) | Descrição | Exemplo/Formato |
| :--- | :--- | :--- | :--- |
| `BizMsgIdr` | `<AppHdr><BizMsgIdr>` | Identificador único da mensagem. | `M[ISPB_Pagador][UUID_23_caracteres]` |
| `EndToEndId` | `<Document><...><PmtId><EndToEndId>` | **Identificador Fim a Fim.** A identidade da transação, usada para conciliação e idempotência. | `E[ISPB_Pagador][Timestamp][UUID_9_caracteres]` |
| `TxId` | `<Document><...><PmtId><TxId>` | Identificador da transação para o usuário final (ex: do QR Code). | `[a-zA-Z0-9]{1,35}` |
| `IntrBkSttlmAmt` | `<Document><...><IntrBkSttlmAmt>` | Valor exato da transação a ser liquidada. | `199.99` (com `Ccy="BRL"`) |
| `Dbtr` & `Cdtr` | `<Document><...><Dbtr>` | Blocos contendo os dados do **Pagador (Dbtr)** e do **Recebedor (Cdtr)**, incluindo Nome, CPF/CNPJ. | Estrutura complexa |
| `DbtrAcct` & `CdtrAcct`| `<Document><...><DbtrAcct>` | Blocos com dados da conta do pagador e recebedor (Agência, Conta, Tipo). A conta do recebedor também contém a Chave PIX (`Prxy`). | Estrutura complexa |
| `LclInstrm` | `<Document><...><MndtRltdInf><...><Prtry>` | **Instrumento Local.** Define como o pagamento foi iniciado. | `MANU`, `DICT`, `QRDN`, `QRES`, `INIC` |
| `Ustrd` | `<Document><...><RmtInf><Ustrd>` | Campo de texto livre para a descrição do pagamento. | `Max140Text` |

## Estrutura de Diretórios do Projeto

```
pixspi/
├── .gitignore                # Arquivos e diretórios ignorados pelo Git.
├── Dockerfile                # Define como construir a imagem Docker da aplicação.
├── docker-compose.yml        # Orquestra a execução do container de forma simplificada.
├── pom.xml                   # Arquivo de build do Maven, com dependências e plugins.
└── src/
    └── main/
        ├── java/
        │   └── com/example/pacs008/
        │       ├── Pacs008Application.java   # Ponto de entrada da aplicação Spring Boot.
        │       ├── controller/               # Camada de API (Endpoints REST).
        │       ├── dto/                      # Data Transfer Objects para a API.
        │       └── service/                  # Lógica de negócio (criação e assinatura do XML).
        └── resources/
            ├── xsd/
            │   └── pacs.008.spi.1.13.xsd # Schema oficial para geração das classes JAXB.
            ├── static/                   # Arquivos do frontend (HTML, CSS, JS).
            └── application.properties    # Configurações do Spring Boot.
```

## Pré-requisitos para Instalação

-   [Git](https://git-scm.com/)
-   [Docker](https://www.docker.com/products/docker-desktop/)
-   [Docker Compose](https://docs.docker.com/compose/install/) (geralmente incluído no Docker Desktop)

Para desenvolvimento local (sem Docker), você também precisará de:
-   [Java JDK 21](https://www.oracle.com/java/technologies/downloads/#java21)
-   [Apache Maven 3.9+](https://maven.apache.org/download.cgi)

## Como Instalar e Executar

A maneira recomendada para executar o projeto é utilizando Docker, pois ele gerencia todas as dependências e configurações de ambiente.

### 1. Executando com Docker (Recomendado)

1.  **Clone o repositório:**
    ```bash
    git clone <URL_DO_SEU_REPOSITORIO>
    cd pixspi
    ```

2.  **Construa a imagem e inicie o container:**
    Execute o comando a seguir na raiz do projeto. Ele irá construir a imagem Docker e iniciar o serviço.
    ```bash
    docker-compose up --build
    ```

3.  **Acesse a aplicação:**
    Após o build e a inicialização do Spring Boot, a aplicação estará disponível no seu navegador no endereço:
    **[http://localhost:8080](http://localhost:8080)**

### 2. Executando Localmente (Alternativa)

1.  **Clone o repositório** (se ainda não o fez).

2.  **Compile o projeto com Maven:**
    Navegue até a raiz do projeto e execute o comando abaixo. Este passo irá baixar as dependências e, crucialmente, **gerar as classes Java a partir do arquivo XSD** usando o plugin JAXB.
    ```bash
    mvn clean install
    ```

3.  **Execute a aplicação:**
    Após a compilação bem-sucedida, inicie o servidor Spring Boot:
    ```bash
    java -jar target/pacs008-0.0.1-SNAPSHOT.jar
    ```

4.  **Acesse a aplicação:**
    A aplicação estará disponível em **[http://localhost:8080](http://localhost:8080)**.

## Como Testar a Aplicação

### 1. Testando pelo Frontend

1.  Abra seu navegador e acesse **[http://localhost:8080](http://localhost:8080)**.
2.  Você verá um formulário pré-preenchido com dados de exemplo para um pagamento PIX.
3.  Altere os campos se desejar ou mantenha os valores padrão.
4.  Clique no botão **"Gerar Mensagem pacs.008"**.
5.  Abaixo do formulário, uma caixa de resultado aparecerá exibindo a mensagem XML `pacs.008` completa e formatada, gerada pelo backend.

### 2. Testando o Endpoint da API (com cURL)

Você pode testar o endpoint REST diretamente usando uma ferramenta como `cURL` ou Postman.

Abra um terminal e execute o comando `cURL` abaixo:

```bash
curl -X POST http://localhost:8080/api/pix/payments \
-H "Content-Type: application/json" \
-H "Accept: application/xml" \
-d '{
    "payerName": "João da Silva Pagador",
    "payerCpfCnpj": "11122233344",
    "payerIspb": "12345678",
    "payerAgency": "0001",
    "payerAccount": "123456",
    "payerAccountType": "CACC",
    "receiverName": "Loja Exemplo LTDA",
    "receiverCpfCnpj": "11222333000144",
    "receiverIspb": "87654321",
    "receiverAgency": "0001",
    "receiverAccount": "654321",
    "receiverPixKey": "loja@exemplo.com",
    "receiverAccountType": "TRAN",
    "amount": 149.90,
    "description": "Compra de material de escritório"
}'
```

A resposta será o conteúdo XML da mensagem `pacs.008`.

---

> ### **Nota Importante sobre a Assinatura Digital**
>
> ⚠️ Esta implementação é um **simulador para fins educacionais e de desenvolvimento**. A funcionalidade de assinatura digital (`SignatureService.java`) é um **stub** (simulação) e **NÃO** realiza uma assinatura criptográfica real. Em um ambiente de produção, seria necessário substituir o código simulado por uma implementação robusta utilizando a API Java XML Digital Signature (XMLDSig) e um certificado digital A1 ou A3 válido, armazenado de forma segura (e.g., HSM ou Java KeyStore).
