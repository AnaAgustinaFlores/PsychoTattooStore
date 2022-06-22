package com.PsychoTeam.Psycho.controllers;

import com.PsychoTeam.Psycho.Dtos.ClientProductDTO;
import com.PsychoTeam.Psycho.Dtos.ProductDTO;
import com.PsychoTeam.Psycho.Models.Client;
import com.PsychoTeam.Psycho.Models.ClientProduct;
import com.PsychoTeam.Psycho.Models.Product;
import com.PsychoTeam.Psycho.services.ClientProductService;
import com.PsychoTeam.Psycho.services.ClientService;
import com.PsychoTeam.Psycho.services.ProductService;
import org.apache.tomcat.util.http.parser.Authorization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.spel.ast.NullLiteral;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.web.servlet.headers.HttpStrictTransportSecurityDsl;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ClientProductController {

    @Autowired
    private ClientProductService clientProductService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ClientService clientService;

    @GetMapping("/cart")
    public List<ProductDTO> getProducts(){
        return productService.getProductsDTO();
    }

    @PatchMapping("/cart/modify")
    public ResponseEntity<Object> modifyProduct(
            @RequestParam int clientProduct_id, @RequestParam int quantity){
        if(clientProductService.getClientProductById(clientProduct_id) == null){
            return new ResponseEntity<>("Invalid data", HttpStatus.FORBIDDEN);
        }
        ClientProduct clientProduct = clientProductService.getClientProductById(clientProduct_id);
        clientProduct.setQuantity(quantity);
        clientProductService.saveClientProduct(clientProduct);
        return new ResponseEntity<>("Product modified correctly", HttpStatus.ACCEPTED);
    }

    @GetMapping("/cart/current")
    public ResponseEntity<Object> getProductsOfClient(Authentication auth){
        if(auth.getName() == null){
            return new ResponseEntity<>("Invalid authentication credentials", HttpStatus.FORBIDDEN);
        }
        Client client = clientService.getClient(auth.getName());
        List<ClientProductDTO> cartListDTO = clientProductService.getClientProductsByClient(client);
        return new ResponseEntity<>(cartListDTO, HttpStatus.ACCEPTED);
    }

    @PostMapping("/cart")
    public ResponseEntity<Object> createProduct(
            @RequestParam String size, @RequestParam String color, @RequestParam int quantity, @RequestParam int id_product, Authentication auth) throws MessagingException, UnsupportedEncodingException {

        if(auth.getName() == null){
            return new ResponseEntity<>("Invalid credentials", HttpStatus.FORBIDDEN);
        }

        if (size.isEmpty() || color.isEmpty())
            return new ResponseEntity<>("Missing data", HttpStatus.FORBIDDEN);

        if (clientService.getClient(auth.getName()) == null || productService.getProductById(id_product) == null)
            return new ResponseEntity<>("Invalid data", HttpStatus.FORBIDDEN);


        Client client = clientService.getClient(auth.getName());
        Product product = productService.getProductById(id_product);
        ClientProduct clientProduct = new ClientProduct(client, product, size, color, product.getPrice(), quantity);

        Set<ClientProduct> productExist = client.getCart().stream()
                .filter(product1 ->  product1.getProduct() == product
                        && product1.getSize().equals(size)
                        && product1.getColor().equals(color)).collect(Collectors.toSet());

        if (productExist.size() > 0){
            ClientProduct clientProduct1 =  productExist.stream().findFirst().orElse(null);
            if(clientProduct1.getProduct().getStock() < clientProduct1.getQuantity() + 1){
                return new ResponseEntity<>("Not enough items in stock", HttpStatus.FORBIDDEN);
            }
            clientProduct1.setQuantity(clientProduct1.getQuantity() + 1);
            // --------------------- //
            Product productEntity = clientProduct1.getProduct();
            productEntity.setStock(productEntity.getStock() - 1);
            productService.saveProduct(productEntity);
            // --------------------- //
            clientProductService.saveClientProduct(clientProduct1);
            return new ResponseEntity<>("Product added successfully",HttpStatus.CREATED);
        }


        client.addProductCart(clientProduct); // Add ClientProduct to Client
        product.addClientProduct(clientProduct); // Add ClientProduct to Product

        clientService.saveClient(client); // Save client
        productService.saveProduct(product); // Save product
        clientProductService.saveClientProduct(clientProduct); // Save ClientProduct

        return new ResponseEntity<>("Product added successfully",HttpStatus.CREATED);
    }

    @DeleteMapping("/cart")
    public ResponseEntity<Object> removeProductFromCart(
            @RequestParam int clientProduct_id){

        if(clientProductService.getClientProductById(clientProduct_id) == null){
            return new ResponseEntity<>("Invalid data", HttpStatus.FORBIDDEN);
        }
        ClientProduct clientProduct = clientProductService.getClientProductById(clientProduct_id);
        clientProductService.removeClientProduct(clientProduct);
        return new ResponseEntity<>("Product removed successfully", HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/cart/empty")
    public ResponseEntity<Object> emptyCart(Authentication auth){
        if(auth.getName() == null){
            return new ResponseEntity<>("Invalid authentication credentials", HttpStatus.FORBIDDEN);
        }

        Client client = clientService.getClient(auth.getName());
        clientProductService.removeClientProducts(client);
        return new ResponseEntity<>("Product removed successfully", HttpStatus.ACCEPTED);
    }


}
