package br.edu.rotavital;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AplicacaoRotaVital {
    public static void main(String[] argumentos) {
        SpringApplication.run(AplicacaoRotaVital.class, argumentos);
    }
    @Bean
    public RotaVitalServico rotaVitalServico() { return new RotaVitalServico(); }
}
