package com.example.EmployeeManagementSystem.Scheduler;

import com.example.EmployeeManagementSystem.Service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeliveryScheduler {
    private static final Logger logger = LoggerFactory.getLogger(DeliveryScheduler.class);
    private final DeliveryService deliveryService;

    public DeliveryScheduler(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    // Runs every hour
    @Scheduled(cron = "0 0 * * * *")
    public void processDeliveries() {
        logger.info("Starting delivery processing scheduler...");
        try {
            deliveryService.processDueDeliveries();
            logger.info("Delivery processing completed successfully");
        } catch (Exception e) {
            logger.error("Error processing deliveries: ", e);
        }
    }
}
