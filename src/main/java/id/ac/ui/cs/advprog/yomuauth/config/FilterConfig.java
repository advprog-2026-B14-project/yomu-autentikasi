package id.ac.ui.cs.advprog.yomuauth.config;

import id.ac.ui.cs.advprog.yomuauth.filter.AdminFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Autowired
    private AdminFilter adminFilter;

    @Bean
    public FilterRegistrationBean<AdminFilter> adminFilterRegistration() {
        FilterRegistrationBean<AdminFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(adminFilter);
        registration.addUrlPatterns("/admin/*");
        return registration;
    }
}