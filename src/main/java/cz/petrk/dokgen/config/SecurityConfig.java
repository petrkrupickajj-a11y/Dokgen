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
 * pres verejnou /registrace. Verejna registrace vzdy zaklada ucet s roli
 * ASISTENTKA (viz RegistraceService) - roli si nikdo nemuze zvolit sam,
 * aby si nemohl udelit vyssi opravneni. Role uctu (ADMIN/ASISTENTKA, viz
 * Role) omezuje opravneni - podrobnosti nize.
 *
 * CSRF ochrana zustava zapnuta (Spring Security default) - Thymeleaf do
 * kazdeho formulare s th:action automaticky vlozi skryte CSRF pole, takze
 * existujici formulare (ulozit, smazat, generovat, sablony...) nepotrebuji
 * zadnou upravu.
 *
 * Role (viz Role, DokgenUserDetailsService) omezuje /sablony jen na ADMIN -
 * ASISTENTKA se k nim nedostane, misto Whitelabel chyby ji accessDeniedHandler
 * posle na srozumitelnou stranku /pristup-odepren (viz PrihlaseniController).
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

    /** Prihlaseny, ale neopravneny uzivatel (napr. ASISTENTKA na /sablony) dostane srozumitelnou stranku misto Whitelabel chyby. */
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
                                "/zapomenute-heslo", "/nove-heslo").permitAll()
                        // Sprava sablon jde jen ADMINovi (viz Role) - ASISTENTKA smi jen
                        // spravovat klienty a generovat dokumenty.
                        .requestMatchers("/sablony", "/sablony/**").hasRole("ADMIN")
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
