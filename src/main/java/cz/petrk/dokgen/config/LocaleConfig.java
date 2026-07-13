package cz.petrk.dokgen.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;

/**
 * Appka umi cesky/anglicky/nemecky - preklady textu jsou v messages*.properties
 * (viz src/main/resources). Zvoleny jazyk appka pamatuje v cookie (prezije i
 * zavreni prohlizece), prepnout jde parametrem ?lang=cs|en|de - to obstarava
 * jazykovy prepinac v navigaci (viz styles.css .jazyky).
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("dokgenJazyk");
        resolver.setDefaultLocale(new Locale("cs"));
        resolver.setCookieMaxAge(java.time.Duration.ofDays(365));
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    // Validacni hlasky na entitach (@NotBlank(message = "{klient.jmeno.povinne}") apod.)
    // by si bez tohohle bean hledaly preklad v samostatnem ValidationMessages.properties -
    // takhle je napojime na stejnou sadu messages*.properties jako zbytek appky.
    @Bean
    public LocalValidatorFactoryBean getValidator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
