package com.example.pacs008.config;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import jakarta.jms.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

/**
 * Configuração do JMS para conectar a um servidor IBM MQ real do SPI.
 * Esta classe é responsável por criar a ConnectionFactory com as propriedades
 * de segurança (mTLS) e conexão necessárias.
 */
@Configuration
@EnableJms
public class JmsConfig {

    // Injeta os valores do application.properties
    @Value("${ibm.mq.host-name}")
    private String host;
    @Value("${ibm.mq.port}")
    private int port;
    @Value("${ibm.mq.queue-manager}")
    private String queueManager;
    @Value("${ibm.mq.channel}")
    private String channel;
    @Value("${ibm.mq.user}")
    private String user;
    @Value("${ibm.mq.password}")
    private String password;
    @Value("${ibm.mq.ssl.cipher-suite}")
    private String sslCipherSuite;
    @Value("${ibm.mq.ssl.key-store}")
    private String keyStorePath;
    @Value("${ibm.mq.ssl.key-store-password}")
    private String keyStorePassword;
    @Value("${ibm.mq.ssl.trust-store}")
    private String trustStorePath;
    @Value("${ibm.mq.ssl.trust-store-password}")
    private String trustStorePassword;

    /**
     * Cria e configura o Bean da ConnectionFactory para o IBM MQ.
     *
     * @return A ConnectionFactory configurada.
     * @throws Exception se houver erro na configuração.
     */
    @Bean
    public ConnectionFactory mqConnectionFactory() throws Exception {
        MQConnectionFactory connectionFactory = new MQConnectionFactory();

        // Configurações básicas de conexão
        connectionFactory.setHostName(host);
        connectionFactory.setPort(port);
        connectionFactory.setQueueManager(queueManager);
        connectionFactory.setChannel(channel);
        connectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT); // Modo Cliente é obrigatório

        // Credenciais
        connectionFactory.setStringProperty(WMQConstants.USERID, user);
        connectionFactory.setStringProperty(WMQConstants.PASSWORD, password);

        // Configuração de Segurança (SSL/mTLS)
        connectionFactory.setSSLCipherSuite(sslCipherSuite);
        
        // Define as propriedades do sistema para os keystores
        // O cliente IBM MQ lê estas propriedades do sistema para configurar o contexto SSL
        System.setProperty("javax.net.ssl.keyStore", trustStorePath.startsWith("classpath:") ? getPathFromClasspath(keyStorePath) : keyStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
        System.setProperty("javax.net.ssl.trustStore", trustStorePath.startsWith("classpath:") ? getPathFromClasspath(trustStorePath) : trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        
        return connectionFactory;
    }

    /**
     * Cria um JmsTemplate que utilizará a ConnectionFactory configurada.
     *
     * @param connectionFactory O bean da ConnectionFactory.
     * @return O JmsTemplate pronto para uso.
     */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }

    /**
     * Helper para obter o caminho absoluto de um recurso no classpath.
     */
    private String getPathFromClasspath(String resourcePath) {
        try {
            return getClass().getClassLoader().getResource(resourcePath.substring("classpath:".length())).getPath();
        } catch (Exception e) {
            throw new RuntimeException("Não foi possível encontrar o recurso no classpath: " + resourcePath, e);
        }
    }
}