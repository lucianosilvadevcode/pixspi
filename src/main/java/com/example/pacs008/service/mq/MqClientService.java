package com.example.pacs008.service.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

/**
 * Cliente de Fila de Mensagens (MQ) para enviar mensagens ao SPI.
 * Utiliza o JmsTemplate do Spring para interagir com o servidor MQ configurado.
 */
@Service
@RequiredArgsConstructor
@Slf4j // Adiciona o logger SLF4J
public class MqClientService {

    private final JmsTemplate jmsTemplate;

    /**
     * Publica uma mensagem em uma fila de destino no servidor MQ.
     *
     * @param message O conteúdo da mensagem (o XML assinado do pacs.008).
     * @param queueName O nome da fila de destino no SPI.
     * @return Uma string confirmando o sucesso da publicação.
     */
    public String publish(String message, String queueName) {
        if (message == null || message.isBlank()) {
            log.error("Tentativa de publicar mensagem vazia na fila {}", queueName);
            throw new IllegalArgumentException("A mensagem para publicação não pode ser vazia.");
        }

        try {
            log.info("Publicando mensagem na fila: {}", queueName);
            
            // Envia a mensagem para a fila especificada
            jmsTemplate.convertAndSend(queueName, message);

            log.info("Mensagem publicada com sucesso na fila: {}", queueName);
            return "Mensagem publicada na fila '" + queueName + "' com sucesso.";

        } catch (Exception e) {
            log.error("Falha ao publicar mensagem na fila {}: {}", queueName, e.getMessage(), e);
            // Em produção, uma exceção mais específica seria lançada para tratamento adequado.
            throw new RuntimeException("Falha na comunicação com o servidor MQ.", e);
        }
    }
}