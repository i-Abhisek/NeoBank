package com.banking.paymentservice.service;

import com.banking.paymentservice.dto.CreatePaymentRequest;
import com.banking.paymentservice.dto.PaymentOrderResponse;
import com.banking.paymentservice.entity.Payment;
import com.banking.paymentservice.entity.PaymentStatus;
import com.banking.paymentservice.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";

    /**
     * Create Razorpay payment order
     *
     * FLOW:
     * 1. Create order in Razorpay
     * 2. Save Payment record
     * 3. Return order details to frontend
     * 4. Frontend shows Razorpay Checkout
     * 5. User pays
     * 6. Razorpay calls webhook
     */
    public PaymentOrderResponse createPaymentOrder(
            CreatePaymentRequest request) throws RazorpayException {

        log.info("Create Payment order for account: {} amount: {}",
                request.getAccountNumber(),
                request.getAmount());

        // Create Razorpay client
        RazorpayClient razorpayClient =
                new RazorpayClient(keyId, keySecret);

        // Convert INR to paise
        int convertedAmount = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .intValue();

        // Create Razorpay order request
        JSONObject orderRequest = new JSONObject();

        orderRequest.put("amount", convertedAmount);
        orderRequest.put("currency", "INR");

        String receipt = "rcpt_" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 10);

        orderRequest.put("receipt", receipt);

        // Create Razorpay order
        Order razorpayOrder =
                razorpayClient.orders.create(orderRequest);

        String razorpayOrderId =
                razorpayOrder.get("id").toString();

        log.info("Payment Order Created: {}", razorpayOrderId);

        // Save payment record
        Payment payment = new Payment();

        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setAccountNumber(request.getAccountNumber());
        payment.setAmount(request.getAmount());
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(request.getDescription());

        Payment savedPayment =
                paymentRepository.save(payment);

        log.info("Payment saved successfully: {}", savedPayment.getId());

        // Return response to frontend
        return new PaymentOrderResponse(
                savedPayment.getId(),
                razorpayOrderId,
                request.getAmount(),
                "INR",
                PaymentStatus.CREATED.toString(),
                keyId
        );
    }
    public void handleWebhook(Map<String, Object> payload) throws RazorpayException {
         log.info("Receive Razorpay Webhook Payload: {}", payload.get("event"));

         String event = (String) payload.get("event");

         if("payment.captured".equals(event)) {
             handlePaymentSuccess(payload);
         }
         else if("payment.failed".equals(event)) {
             handlePaymentFailure(payload);
         }
    }

    private void handlePaymentSuccess(Map<String, Object> payload)  {

        try{

            Map<String, Object> paymentData = extractPaymentData(payload);
            String orderId = (String)paymentData.get("order_id");
            String paymentId = (String)paymentData.get("id");

            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
            .orElseThrow(() -> new RuntimeException(
                    "Payment not found for order:"+orderId
            ));

            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            // Publish payment completed event

            Map<String,Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("razorPaymentId", paymentId);

            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC,payment.getId(),event);
            log.info("Payment Completed successfully: {}", payment.getId());

        }
        catch (Exception e){
            log.error("Error handling  payment success: {}",e.getMessage());
        }
    }

    private void handlePaymentFailure(Map<String, Object> payload) {

        try{
            Map<String,Object> paymentData = extractPaymentData(payload);
            String orderId = (String)paymentData.get("order_id");

            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException(
                            "Payment not found for order:"+orderId
                    ));
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("payment failed via Razorpay");
            paymentRepository.save(payment);

            // Publish payment completed event

            Map<String,Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("reason", "Payment failed via Razorpay");

            kafkaTemplate.send(PAYMENT_FAILED_TOPIC,payment.getId(),event);

            log.warn("Payment Failed successfully: {}", payment.getId());
        }
        catch (Exception e){

            log.error("Error handling  payment failure: {}",e.getMessage());
        }

    }

    private Map<String, Object> extractPaymentData(Map<String, Object> payload) {
        Map<String, Object> event = (Map<String, Object>) payload.get("event");

        Map<String, Object> paymentWrapper = (Map<String, Object>) event.get("payment");
        return (Map<String, Object>) paymentWrapper.get("entity");
    }
}