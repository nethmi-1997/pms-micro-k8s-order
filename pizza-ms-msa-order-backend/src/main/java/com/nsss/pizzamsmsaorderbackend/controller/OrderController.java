package com.nsss.pizzamsmsaorderbackend.controller;

import com.nsss.pizzamsmsaorderbackend.model.Crust;
import com.nsss.pizzamsmsaorderbackend.model.Order;
import com.nsss.pizzamsmsaorderbackend.model.OrderItem;
import com.nsss.pizzamsmsaorderbackend.model.Topping;
import com.nsss.pizzamsmsaorderbackend.response.MessageResponse;
import com.nsss.pizzamsmsaorderbackend.repository.OrderRepository;
import com.nsss.pizzamsmsaorderbackend.request.GetCrustRequest;
import com.nsss.pizzamsmsaorderbackend.request.GetToppingRequest;
import com.nsss.pizzamsmsaorderbackend.request.OrderItemRequest;
import com.nsss.pizzamsmsaorderbackend.request.OrderRequest;
import com.nsss.pizzamsmsaorderbackend.request.ToppingStatisticsRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    RestTemplate restTemplate;

    @PostMapping("/orders")
    public ResponseEntity<?> addOrder(@Valid @RequestBody OrderRequest orderRequest) {
        double grandTotal = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        Order order = new Order(
                orderRequest.getCustomerName(),
                orderRequest.getAddress(),
                orderRequest.getDeliveryRider(),
                orderRequest.getCashier(),
                new Date()
        );

        if(orderRequest.getDeliveryRider().trim().isEmpty()) {
            order.setDeliveryAssigned(false);
        } else {
            order.setDeliveryAssigned(true);
        }

        for(OrderItemRequest orderItemRequest : orderRequest.getItems()) {
            OrderItem orderItem = new OrderItem();
            double crustPrice;
            double toppingPrice;
            double total;

            orderItem.setCrust(orderItemRequest.getCrust());
            orderItem.setTopping(orderItemRequest.getTopping());
            orderItem.setSize(orderItemRequest.getSize());
            orderItem.setQuantity(orderItemRequest.getQuantity());

            // Optional<Crust> crust = Optional.of(restTemplate.getForObject("http://CRUST-SERVICE/crust/crusts" +  orderItemRequest.getCrust(), Crust.class));
            // Optional<Topping> topping = Optional.of(restTemplate.getForObject("http://TOPPING-SERVICE/topping/toppings" +  orderItemRequest.getTopping(), Topping.class));

            GetCrustRequest getCrustRequest = new GetCrustRequest();
            getCrustRequest.setName(orderItemRequest.getCrust());
            Crust tempCrust = restTemplate.postForEntity("http://CRUST-SERVICE/crust/crusts/search", getCrustRequest, Crust.class).getBody();
            Optional<Crust> crust = Optional.of(tempCrust);

            GetToppingRequest getToppingRequest = new GetToppingRequest();
            getToppingRequest.setName(orderItemRequest.getTopping());
            Topping tempTopping = restTemplate.postForEntity("http://TOPPING-SERVICE/topping/toppings/search", getToppingRequest, Topping.class).getBody();
            Optional<Topping> topping = Optional.of(tempTopping);

            if (orderItemRequest.getSize().equals("small")) {
                crustPrice = crust.get().getSmallPrice();
                toppingPrice = topping.get().getSmallPrice();
                total = crustPrice + toppingPrice;

                grandTotal += total*orderItemRequest.getQuantity();

                orderItem.setUnitPrice(total);
                orderItem.setTotal(total*orderItemRequest.getQuantity());
            } else if (orderItemRequest.getSize().equals("medium")) {
                crustPrice = crust.get().getMediumPrice();
                toppingPrice = topping.get().getMediumPrice();
                total = crustPrice + toppingPrice;

                grandTotal += total*orderItemRequest.getQuantity();

                orderItem.setUnitPrice(total);
                orderItem.setTotal(total*orderItemRequest.getQuantity());
            } else {
                crustPrice = crust.get().getLargePrice();
                toppingPrice = topping.get().getLargePrice();
                total = crustPrice + toppingPrice;

                grandTotal += total*orderItemRequest.getQuantity();

                orderItem.setUnitPrice(total);
                orderItem.setTotal(total*orderItemRequest.getQuantity());
            }

            orderItems.add(orderItem);
        }

        order.setItems(orderItems);
        order.setAmount(grandTotal);

        order.setDelivered(false);

        orderRepository.save(order);

        return ResponseEntity.ok(new MessageResponse("Order added successfully"));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(required = false) String customerName) {
        try {
            List<Order> orders = new ArrayList<>();

            if (customerName == null)
                orderRepository.findAll().forEach(orders::add);
            else
                orderRepository.findAllByCustomerName(customerName).forEach(orders::add);

            if (orders.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(orders, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/orders/delivered/false")
    public ResponseEntity<List<Order>> getAllUndeliveredOrders(@RequestParam(required = false) String riderName) {
        try {
            List<Order> orders = new ArrayList<>();

            if(riderName == null){
                orderRepository.findAllByDeliveredIsFalse().forEach(orders::add);
            }else {
                orderRepository.findAllByDeliveryRiderContainingAndDeliveredIsFalse(riderName).forEach(orders::add);
            }

            return new ResponseEntity<>(orders, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/orders/delivered")
    public ResponseEntity<List<Order>> getAllDeliveredOrders(@RequestParam(required = false) String riderName) {
        try {
            List<Order> orders = new ArrayList<>();

            if(riderName == null){
                orderRepository.findAllByDeliveredIsTrue().forEach(orders::add);
            }else {
                orderRepository.findAllByDeliveryRiderContainingAndDeliveredIsTrue(riderName).forEach(orders::add);
            }

            return new ResponseEntity<>(orders, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<HttpStatus> deleteOrder(@PathVariable("id") String id) {
        try {
            orderRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/orders/delivered/{id}")
    public ResponseEntity<Order> updateOrderDeliveredStatus(@PathVariable("id") String id) {
        Optional<Order> orderData = orderRepository.findById(id);

        if(orderData.isPresent()) {
            Order _order = orderData.get();
            _order.setDelivered(true);
            _order.setDeliveryTimestamp(new Date());

            return new ResponseEntity<>(orderRepository.save(_order), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/orders/delivery/assigned/false/{id}")
    public ResponseEntity<Order> updateOrderSetDeliveryAssignedStatusFalse(@PathVariable("id") String id) {
        Optional<Order> orderData = orderRepository.findById(id);

        if(orderData.isPresent()) {
            Order _order = orderData.get();
            _order.setDeliveryAssigned(false);
            _order.setDeliveryRider("");
            _order.setDelivered(false);

            return new ResponseEntity<>(orderRepository.save(_order), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/orders/between")
    public ResponseEntity<List<Order>> getAllByOrdersTimestampBetween(@Valid @RequestBody ToppingStatisticsRequest toppingStatisticsRequest) {
        try {
            List<Order> orders = new ArrayList<>();

            orderRepository.findAllByOrderTimestampBetween(toppingStatisticsRequest.getFromTimestamp(), toppingStatisticsRequest.getToTimestamp()).forEach(orders::add);

            return new ResponseEntity<>(orders, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}