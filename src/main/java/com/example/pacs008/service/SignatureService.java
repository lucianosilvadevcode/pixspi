package com.example.pacs008.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

/**
 * Serviço responsável por realizar a assinatura digital de mensagens XML
 * utilizando o padrão XMLDSig (Enveloped Signature).
 * Esta é uma implementação de produção que utiliza um keystore JKS.
 */
@Service
public class SignatureService {

    @Value("${ibm.mq.ssl.key-store}")
    private String keyStorePath;
    @Value("${ibm.mq.ssl.key-store-password}")
    private String keyStorePassword;
    @Value("${ibm.mq.ssl.key-alias}")
    private String keyAlias;
    @Value("${ibm.mq.ssl.key-password}")
    private String keyPassword;

    /**
     * Assina um documento XML utilizando a chave privada do keystore configurado.
     * O método implementa o padrão "Enveloped Signature", onde a tag <Signature>
     * é inserida dentro do próprio documento que ela assina.
     *
     * @param xmlContent O conteúdo XML original (sem assinatura) como uma String.
     * @return O conteúdo XML com a tag <Signature> adicionada, como uma String.
     * @throws Exception se ocorrer qualquer erro durante o processo de assinatura,
     *                   seja no carregamento do keystore, na manipulação do XML
     *                   ou na operação criptográfica.
     */
    public String signXml(String xmlContent) throws Exception {
        // 1. Carregar a chave privada e o certificado do Keystore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        // Remove "classpath:" para carregar como recurso do sistema
        String path = keyStorePath.substring("classpath:".length());
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Keystore não encontrado em: " + keyStorePath);
            }
            keyStore.load(is, keyStorePassword.toCharArray());
        }

        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(keyAlias);

        // 2. Parsear o XML de String para um Documento DOM
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true); // Essencial para XMLDSig
        Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xmlContent)));

        // 3. Preparar o contexto da assinatura
        // A assinatura será inserida dentro da tag <Sgntr>
        DOMSignContext dsc = new DOMSignContext(privateKey, doc.getElementsByTagName("Sgntr").item(0));

        // 4. Criar a fábrica de assinaturas XML
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // 5. Criar uma Referência para o documento a ser assinado (todo o documento)
        // O Transform.ENVELOPED é crucial para que a própria tag de assinatura seja excluída do cálculo do hash.
        Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA256, null),
                Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                null, null);

        // 6. Criar o SignedInfo, que agrupa a referência e os métodos
        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null), // RSA-SHA256
                Collections.singletonList(ref));

        // 7. Adicionar o certificado público à assinatura (KeyInfo)
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        X509Data x509Data = kif.newX509Data(List.of(certificate.getSubjectX500Principal().getName(), certificate));
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(x509Data));

        // 8. Criar a assinatura XML e assiná-la
        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(dsc);

        // 9. Converter o Documento DOM assinado de volta para String
        StringWriter sw = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();
        trans.transform(new DOMSource(doc), new StreamResult(sw));

        return sw.toString();
    }
}