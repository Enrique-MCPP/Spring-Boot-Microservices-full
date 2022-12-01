package frm.utn;


import frm.utn.entities.Supplier;

import frm.utn.repositories.SupplierRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


import java.util.stream.Stream;

@SpringBootApplication
public class SupplierServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupplierServiceApplication.class, args);
    }
    @Bean
    CommandLineRunner start(SupplierRepository supplierRepository) {
        return args -> {



            Stream.of("IBM", "HP", "Apple").forEach(name -> {
                supplierRepository.save(new Supplier( name, "contact@"+name+".fr"));
            });
            System.out.println("----- SUPPLIERS -----");
            supplierRepository.findAll().forEach(supplier -> {
                System.out.println(supplier.getName());
            });
          


        };

    }



}
