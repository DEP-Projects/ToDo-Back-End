package lk.ijse.dep.web.api;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lk.ijse.dep.web.dto.UserDTO;
import lk.ijse.dep.web.util.AppUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.crypto.SecretKey;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

@WebServlet(name = "UserServlet", urlPatterns = {"/api/v1/users/*","/api/v1/auth"})
public class UserServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Jsonb jsonb = JsonbBuilder.create();
        try {
            UserDTO userDTO = jsonb.fromJson(request.getReader(), UserDTO.class);
            if (userDTO.getUsername() == null || userDTO.getPassword() == null || userDTO.getUsername().trim().isEmpty() || userDTO.getPassword().trim().isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
            try (Connection connection = cp.getConnection()) {

                if (request.getServletPath().equals("/api/v1/auth")) {
                    PreparedStatement pstm = connection.
                            prepareStatement("SELECT * FROM `user` WHERE username=?");
                    pstm.setObject(1, userDTO.getUsername());
                    ResultSet rst = pstm.executeQuery();
                    if (rst.next()) {
                        String sha256Hex = DigestUtils.sha256Hex(userDTO.getPassword());
                        if (sha256Hex.equals(rst.getString("password"))) {

                            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(AppUtil.getAppSecurityKey()));
                            String jws = Jwts.builder()
                                    .setIssuer("ijse")
                                    .setExpiration(new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24)))
                                    .setIssuedAt(new java.util.Date())
                                    .claim("name", userDTO.getUsername())
                                    .signWith(key)
                                    .compact();

                            response.setContentType("text/plain");
                            response.getWriter().println(jws);

                        } else {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        }
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                } else {
                    PreparedStatement pstm = connection.prepareStatement("SELECT * FROM `user` WHERE username=?");
                    pstm.setObject(1, userDTO.getUsername());
                    if (pstm.executeQuery().next()) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().println("User already exists");
                        return;
                    }
                    pstm = connection.prepareStatement("INSERT INTO `user` VALUES (?,?)");
                    pstm.setObject(1, userDTO.getUsername());
                    String sha256Hex = DigestUtils.sha256Hex(userDTO.getPassword());
                    pstm.setObject(2, sha256Hex);
                    if (pstm.executeUpdate() > 0) {
                        response.setStatus(HttpServletResponse.SC_CREATED);
                    } else {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                }
            } catch (SQLException throwables) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                throwables.printStackTrace();
            }
        } catch (JsonbException exp) {
            exp.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
