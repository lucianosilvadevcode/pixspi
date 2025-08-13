package com.example.pacs008.service;

import br.gov.bcb.pi.pacs008.v1.*;
import com.example.pacs008.dto.PaymentRequestDto;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.StringWriter;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * Serviço responsável por criar, preencher e processar a mensagem pacs.008.
 */
@Service
@RequiredArgsConstructor
public class Pacs008Service {

    private final SignatureService signatureService;
    private static final String BCB_ISPB = "00000000";
    private static final String SETTLEMENT_METHOD = "CLRG";
    private static final String LOCAL_INSTRUMENT = "MANU"; // Iniciação Manual como exemplo

    /**
     * Cria e processa uma mensagem pacs.008 a partir dos dados de uma solicitação.
     * O método constrói a estrutura completa do XML, preenche os campos obrigatórios
     * e realiza o marshalling (conversão de objeto para XML).
     *
     * @param request O DTO contendo os dados do pagamento.
     * @return A string XML da mensagem pacs.008, pronta para ser assinada e enviada.
     * @throws Exception se ocorrer um erro durante a geração do XML.
     */
    public String createAndProcessPacs008Message(PaymentRequestDto request) throws Exception {
        ObjectFactory factory = new ObjectFactory();
        SPIEnvelopeMessage envelope = factory.createSPIEnvelopeMessage();

        // 1. Construir o Cabeçalho (AppHdr)
        envelope.setAppHdr(createHeader(factory, request.getPayerIspb()));

        // 2. Construir o Documento (Document)
        SPIpacs00800109 document = factory.createSPIpacs00800109();
        FIToFICustomerCreditTransferV09 creditTransfer = factory.createFIToFICustomerCreditTransferV09();
        document.setFIToFICstmrCdtTrf(creditTransfer);
        envelope.setDocument(document);

        // 2.1. Cabeçalho do Grupo (GrpHdr)
        creditTransfer.setGrpHdr(createGroupHeader(factory, envelope.getAppHdr().getBizMsgIdr()));

        // 2.2. Informações da Transação (CdtTrfTxInf)
        creditTransfer.getCdtTrfTxInf().add(createTransactionInfo(factory, request, envelope.getAppHdr().getBizMsgIdr()));

        // 3. Marshalling para XML
        StringWriter sw = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SPIEnvelopeMessage.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.marshal(envelope, sw);
        
        String xmlContent = sw.toString();

        // 4. Assinar o XML (Etapa simulada)
        return signatureService.signXml(xmlContent);
    }

    // Métodos auxiliares para criar partes da mensagem
    
    private SPIhead00100101 createHeader(ObjectFactory factory, String fromIspb) throws Exception {
        SPIhead00100101 header = factory.createSPIhead00100101();
        
        // Remetente (PSP Pagador) e Destinatário (SPI/BCB)
        Party9Choice from = factory.createParty9Choice();
        from.setFIId(createFinancialInstitution(factory, fromIspb));
        header.setFr(from);

        Party9Choice to = factory.createParty9Choice();
        to.setFIId(createFinancialInstitution(factory, BCB_ISPB));
        header.setTo(to);

        header.setBizMsgIdr(generateMsgId(fromIspb));
        header.setMsgDefIdr("pacs.008.spi.1.13");
        header.setCreDt(getCurrentXmlTimestamp());
        
        // A assinatura será adicionada após o marshalling
        header.setSgntr(factory.createSignatureEnvelope());

        return header;
    }

    private GroupHeader93 createGroupHeader(ObjectFactory factory, String msgId) throws Exception {
        GroupHeader93 grpHdr = factory.createGroupHeader93();
        grpHdr.setMsgId(msgId);
        grpHdr.setCreDtTm(getCurrentXmlTimestamp());
        grpHdr.setNbOfTxs("1");

        SettlementInstruction7 sttlmInf = factory.createSettlementInstruction7();
        sttlmInf.setSttlmMtd(SettlementMethod1Code.fromValue(SETTLEMENT_METHOD));
        grpHdr.setSttlmInf(sttlmInf);

        PaymentTypeInformation28 pmtTpInf = factory.createPaymentTypeInformation28();
        pmtTpInf.setInstrPrty(Priority2Code.HIGH);
        ServiceLevel8Choice slv = factory.createServiceLevel8Choice();
        slv.setPrtry(PrtrySvcLvlCode.PAGPRI);
        pmtTpInf.setSvcLvl(slv);
        grpHdr.setPmtTpInf(pmtTpInf);
        
        return grpHdr;
    }
    
    private CreditTransferTransaction43 createTransactionInfo(ObjectFactory factory, PaymentRequestDto request, String msgId) throws Exception {
        CreditTransferTransaction43 txInfo = factory.createCreditTransferTransaction43();

        // Ids
        PaymentIdentification13 pmtId = factory.createPaymentIdentification13();
        pmtId.setEndToEndId(generateEndToEndId(request.getPayerIspb()));
        pmtId.setTxId("TXID1234567890"); // Geralmente vem de um QR Code ou da iniciação
        txInfo.setPmtId(pmtId);

        // Valor
        ActiveCurrencyAndAmount amount = factory.createActiveCurrencyAndAmount();
        amount.setValue(request.getAmount());
        amount.setCcy(ActiveCurrencyCode.BRL);
        txInfo.setIntrBkSttlmAmt(amount);
        txInfo.setAccptncDtTm(getCurrentXmlTimestamp());
        txInfo.setChrgBr(ChargeBearerType1Code.SLEV);
        
        // Informação de Iniciação
        CreditTransferMandateData1 mndt = factory.createCreditTransferMandateData1();
        MandateTypeInformation2 mndtTp = factory.createMandateTypeInformation2();
        LocalInstrument2Choice lcl = factory.createLocalInstrument2Choice();
        lcl.setPrtry(PrtryLclInstrmCode.fromValue(LOCAL_INSTRUMENT));
        mndtTp.setLclInstrm(lcl);
        mndt.setTp(mndtTp);
        txInfo.setMndtRltdInf(mndt);
        
        // Pagador (Dbtr)
        NmIdPrivateIdentification dbtr = factory.createNmIdPrivateIdentification();
        dbtr.setNm(request.getPayerName());
        PrivateIdentification dbtrId = factory.createPrivateIdentification();
        PersonIdentification13 dbtrPrvtId = factory.createPersonIdentification13();
        GenericPersonIdentification1 dbtrGenericId = factory.createGenericPersonIdentification1();
        dbtrGenericId.setId(request.getPayerCpfCnpj());
        dbtrPrvtId.setOthr(dbtrGenericId);
        dbtrId.setPrvtId(dbtrPrvtId);
        dbtr.setId(dbtrId);
        txInfo.setDbtr(dbtr);
        
        CashAccount38DbtrAcct dbtrAcct = factory.createCashAccount38DbtrAcct();
        dbtrAcct.setId(createAccount(factory, request.getPayerAccount(), request.getPayerAgency()));
        dbtrAcct.setTp(createAccountType(factory, request.getPayerAccountType()));
        txInfo.setDbtrAcct(dbtrAcct);
        txInfo.setDbtrAgt(createFinancialInstitution(factory, request.getPayerIspb()));
        
        // Recebedor (Cdtr)
        IdPrivateIdentification cdtr = factory.createIdPrivateIdentification();
        PrivateIdentification cdtrId = factory.createPrivateIdentification();
        PersonIdentification13 cdtrPrvtId = factory.createPersonIdentification13();
        GenericPersonIdentification1 cdtrGenericId = factory.createGenericPersonIdentification1();
        cdtrGenericId.setId(request.getReceiverCpfCnpj());
        cdtrPrvtId.setOthr(cdtrGenericId);
        cdtrId.setPrvtId(cdtrPrvtId);
        cdtr.setId(cdtrId);
        txInfo.setCdtr(cdtr);
        
        CashAccount38CdtrAcct cdtrAcct = factory.createCashAccount38CdtrAcct();
        cdtrAcct.setId(createAccount(factory, request.getReceiverAccount(), request.getReceiverAgency()));
        cdtrAcct.setTp(createAccountType(factory, request.getReceiverAccountType()));
        ProxyAccountIdentification1 proxy = factory.createProxyAccountIdentification1();
        proxy.setId(request.getReceiverPixKey());
        cdtrAcct.setPrxy(proxy);
        txInfo.setCdtrAcct(cdtrAcct);
        txInfo.setCdtrAgt(createFinancialInstitution(factory, request.getReceiverIspb()));

        // Propósito
        Purpose2Choice purp = factory.createPurpose2Choice();
        purp.setCd(ExternalPurpose1Code.IPAY); // Pagamento de produto/serviço
        txInfo.setPurp(purp);
        
        // Descrição
        RemittanceInformation16 rmtInf = factory.createRemittanceInformation16();
        rmtInf.setUstrd(request.getDescription());
        txInfo.setRmtInf(rmtInf);
        
        return txInfo;
    }

    // Funções utilitárias

    private BranchAndFinancialInstitutionIdentification6 createFinancialInstitution(ObjectFactory factory, String ispb) {
        BranchAndFinancialInstitutionIdentification6 fi = factory.createBranchAndFinancialInstitutionIdentification6();
        FinancialInstitutionIdentification18 fiId = factory.createFinancialInstitutionIdentification18();
        ClearingSystemMemberIdentification2 clrId = factory.createClearingSystemMemberIdentification2();
        clrId.setMmbId(ispb);
        fiId.setClrSysMmbId(clrId);
        fi.setFinInstnId(fiId);
        return fi;
    }

    private AccountIdentification4Choice createAccount(ObjectFactory factory, String account, String agency) {
        AccountIdentification4Choice acctId = factory.createAccountIdentification4Choice();
        GenericAccountIdentification1 othr = factory.createGenericAccountIdentification1();
        othr.setId(new BigInteger(account));
        othr.setIssr(new BigInteger(agency));
        acctId.setOthr(othr);
        return acctId;
    }

    private CashAccountType2Choice createAccountType(ObjectFactory factory, String type) {
        CashAccountType2Choice acctTp = factory.createCashAccountType2Choice();
        acctTp.setCd(ExternalCashAccountType1Code.fromValue(type));
        return acctTp;
    }

    private String generateMsgId(String ispb) {
        String uuidPart = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 23).toUpperCase();
        return "M" + ispb + uuidPart;
    }

    private String generateEndToEndId(String ispb) {
        String timestamp = ZonedDateTime.now().toInstant().toString().replaceAll("[^0-9]", "").substring(0, 14);
        String uuidPart = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 9).toUpperCase();
        return "E" + ispb + timestamp + uuidPart;
    }

    private XMLGregorianCalendar getCurrentXmlTimestamp() throws Exception {
        GregorianCalendar gcal = GregorianCalendar.from(ZonedDateTime.now());
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
    }
}