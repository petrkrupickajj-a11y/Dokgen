package cz.petrk.dokgen.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;

import java.time.Clock;

/**
 * Cela appka je za prihlasenim - obsahuje osobni udaje klientu (jmena,
 * adresy, telefony, ICO), takze nesmi byt volne pristupna komukoliv na siti.
 *
 * Prihlasovacim identifikatorem je email (formLogin.usernameParameter, viz
 * nize) - prihlasovaci ucty se ctou z databaze (entita Uzivatel, viz
 * DokgenUserDetailsService), jednak vestavene z application.properties
 * (UzivateleSeeder je pri prvnim startu naplni do DB), jednak nove pridane
 * pres verejnou /registrace. Kazdy prihlaseny ucet ma stejna opravneni -
 * zadne role se nerozlisuji.
 *
 * CSRF ochrana zustava zapnuta (Spring Security default) - Thymeleaf do
 * kazdeho formulare s th:action automaticky vlozi skryte CSRF pole, takze
 * existujici formulare (ulozit, smazat, generovat, sablony...) nepotrebuji
 * zadnou upravu.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(UzivateleProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Vlastni bean, aby sel v testech nahradit pevnym casem (viz PrihlaseniOmezovac). */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Kdyby autorizace z nejakeho duvodu selhala (napr. CSRF u prihlaseneho uzivatele), zobrazi se srozumitelna stranka misto Whitelabel chyby. */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        AccessDeniedHandlerImpl handler = new AccessDeniedHandlerImpl();
        handler.setErrorPage("/pristup-odepren");
        return handler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(autorizace -> autorizace
                        // Staticky CSS a prepinac jazyka musi jit natahnout i na neprihlasenych
                        // strankach (login, registrace, zapomenute heslo), jinak by byly
                        // nestylovane a neslo by na nich prepnout jazyk pred prihlasenim.
                        //
                        // /login je tu explicitne navic, i kdyz ho formLogin(...).permitAll() nize
                        // taky povoluje - Spring Security registruje permitAll pro presnou cestu
                        // bez ohledu na query retezec, ale kombinace s LocaleChangeInterceptor
                        // (parametr ?lang=) na GET /login vedla k neocekavanemu presmerovani
                        // (ExceptionTranslationFilter to bralo jako neautorizovany pozadavek).
                        // Vlastni requestMatchers tady to spolehlive obejde.
                        .requestMatchers("/styles.css", "/jazyky.js", "/login", "/registrace",
                                "/zapomenute-heslo", "/nove-heslo", "/zdravi").permitAll()
                        .anyRequest().authenticated())
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?odhlaseno")
                        .permitAll())
                .exceptionHandling(handling -> handling.accessDeniedHandler(accessDeniedHandler()));
        return http.build();
    }
}
