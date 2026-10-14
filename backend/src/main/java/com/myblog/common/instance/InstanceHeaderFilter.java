package com.myblog.common.instance;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 응답마다 이 요청을 받은 앱 이름(X-Instance: a / b)을 붙인다 (specs/011). 트래픽이 두 앱에 나뉘는지,
 * 배포 중에 어느 앱이 받았는지 바깥에서 확인하는 용도다. 보안 필터보다 앞에 두어 거절된 요청에도 붙는다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InstanceHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Instance";

    private static final Logger log = LoggerFactory.getLogger(InstanceHeaderFilter.class);

    private final InstanceProperties instance;

    public InstanceHeaderFilter(InstanceProperties instance) {
        this.instance = instance;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !instance.named();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader(HEADER, instance.name());
        chain.doFilter(request, response);
    }

    @EventListener(ApplicationReadyEvent.class)
    void announce() {
        if (instance.named()) {
            log.info("앱 {}로 켜졌습니다", instance.name());
        }
    }
}
