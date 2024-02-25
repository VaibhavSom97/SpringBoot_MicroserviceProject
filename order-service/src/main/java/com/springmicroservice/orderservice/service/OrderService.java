package com.springmicroservice.orderservice.service;

import com.springmicroservice.orderservice.dto.InventoryResponse;
import com.springmicroservice.orderservice.dto.OrderLineItemsDto;
import com.springmicroservice.orderservice.dto.OrderRequest;
import com.springmicroservice.orderservice.model.Order;
import com.springmicroservice.orderservice.model.OrderLineItems;
import com.springmicroservice.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;

    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        //OrderNumber , OrderLineIteamsDto is request body we need to store them in db

        List<OrderLineItems> orderLineItems =  orderRequest.getOrderLineItemsDtoList().stream().map(this::mapToDto).toList();
        order.setOrderLineItemsList(orderLineItems);//store orderLineIteamsList inside order object

        List<String> skuCodes =  order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();

        //Call inventory service and placed order if product is in stock.
        InventoryResponse[] inventoryResponsesArray = webClient.get()
                        .uri("http://localhost:8082/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())//uriBuilder allows you to construct URIs dynamically by chaining method calls.
                        .retrieve()
                        .bodyToMono(InventoryResponse[].class).block();//is used to parse the response body as a Mono<String> or return type bodyToMono is a powerful tool in Spring WebFlux for handling response bodies in a reactive and non-blocking way

        boolean allProductsInStock = Arrays.stream(inventoryResponsesArray).allMatch(InventoryResponse::isInStock);//Check wheather all product is in stock or not inside inventory if one product also missing then it return false

        if(allProductsInStock){
            orderRepository.save(order);
        }else{
            throw new IllegalArgumentException("Product is not in stock, Please try again later");
        }



    }


    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto){
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
