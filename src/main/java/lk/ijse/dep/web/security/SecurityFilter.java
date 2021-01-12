package lk.ijse.dep.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lk.ijse.dep.web.util.AppUtil;

import javax.crypto.SecretKey;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "SecurityFilter", servletNames = {"TodoItemServlet","UserServlet"})
public class SecurityFilter extends HttpFilter {

    public void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws ServletException, IOException {
        if (req.getServletPath().equals("/api/v1/auth") && req.getMethod().equals("POST")) {
            chain.doFilter(req, resp);
        } else if (req.getServletPath().equals("/api/v1/users") && req.getMethod().equals("POST")) {
            chain.doFilter(req, resp);
        } else {

            String authorization = req.getHeader("Authorization");
            if (!authorization.startsWith("Bearer") || authorization==null) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                String token = authorization.replace("Bearer", "");
                Jws<Claims> jws;

                try {
                    SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(AppUtil.getAppSecurityKey()));
                    jws = Jwts.parserBuilder()  // (1)
                            .setSigningKey(key)         // (2)
                            .build()                    // (3)
                            .parseClaimsJws(token); // (4)

                    // we can safely trust the JWT
                    req.setAttribute("user", jws.getBody().get("name"));
                } catch (JwtException ex) {       // (5)
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    // we *cannot* use the JWT as intended by its creator
                }
            }

        }
    }
}

