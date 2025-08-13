document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('payment-form');
    const resultContainer = document.getElementById('result-container');
    const xmlOutput = document.getElementById('xml-output');
    const submitBtn = document.getElementById('submit-btn');

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        submitBtn.disabled = true;
        submitBtn.textContent = 'Gerando...';
        resultContainer.classList.add('hidden');

        // Coletar dados do formulário
        const paymentData = {
            payerName: document.getElementById('payerName').value,
            payerCpfCnpj: document.getElementById('payerCpfCnpj').value,
            payerIspb: document.getElementById('payerIspb').value,
            payerAgency: document.getElementById('payerAgency').value,
            payerAccount: document.getElementById('payerAccount').value,
            payerAccountType: document.getElementById('payerAccountType').value,
            receiverName: document.getElementById('receiverName').value,
            receiverCpfCnpj: document.getElementById('receiverCpfCnpj').value,
            receiverIspb: document.getElementById('receiverIspb').value,
            receiverAgency: document.getElementById('receiverAgency').value,
            receiverAccount: document.getElementById('receiverAccount').value,
            receiverPixKey: document.getElementById('receiverPixKey').value,
            receiverAccountType: document.getElementById('receiverAccountType').value,
            amount: parseFloat(document.getElementById('amount').value),
            description: document.getElementById('description').value,
        };

        try {
            // Fazer a requisição POST para o backend
            const response = await fetch('/api/pix/payments', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/xml'
                },
                body: JSON.stringify(paymentData)
            });

            const xmlText = await response.text();

            if (!response.ok) {
                throw new Error(`Erro do servidor: ${response.status}\n${xmlText}`);
            }
            
            // Exibir o resultado
            xmlOutput.textContent = xmlText;
            resultContainer.classList.remove('hidden');

        } catch (error) {
            console.error('Falha na requisição:', error);
            xmlOutput.textContent = `Não foi possível gerar a mensagem.\nErro: ${error.message}`;
            resultContainer.classList.remove('hidden');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Gerar Mensagem pacs.008';
        }
    });
});