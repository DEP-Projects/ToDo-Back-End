package lk.ijse.dep.web.api;

import lk.ijse.dep.web.dto.TodoItemDTO;
import org.apache.commons.dbcp2.BasicDataSource;
import lk.ijse.dep.web.util.Priority;
import lk.ijse.dep.web.util.Status;

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
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "TodoItemServlet", urlPatterns = "/api/v1/items/*")
public class TodoItemServlet extends HttpServlet {


    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Jsonb jsonb = JsonbBuilder.create();
        try {
            TodoItemDTO item = jsonb.fromJson(req.getReader(), TodoItemDTO.class);
            if (item.getId() != null || item.getText() == null || item.getUsername() == null ||
                    item.getText().trim().isEmpty() || item.getUsername().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
            try (Connection connection = cp.getConnection()) {
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM `user` WHERE username=?");
                pstm.setObject(1, item.getUsername());
                if (!pstm.executeQuery().next()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.setContentType("text/plain");
                    resp.getWriter().println("invalid user");
                    return;
                }
                pstm = connection.prepareStatement("INSERT INTO todo_item (`text`, `priority`, `status`, `username`) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                pstm.setObject(1, item.getText());
                pstm.setObject(2, item.getPriority().toString());
                pstm.setObject(3, item.getStatus().toString());
                pstm.setObject(4, item.getUsername());
                if (pstm.executeUpdate() > 0) {
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    ResultSet generatedKeys = pstm.getGeneratedKeys();
                    generatedKeys.next();
                    int generatedId = generatedKeys.getInt(1);
                    item.setId(generatedId);
                    resp.setContentType("application/json");
                    resp.getWriter().println(jsonb.toJson(item));
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        } catch (JsonbException | SQLException exp) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            exp.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        Jsonb jsonb = JsonbBuilder.create();
        if (request.getPathInfo() == null) {
            //fetch all items

            try (Connection connection = cp.getConnection()) {

                request.setAttribute("user", "yohan");
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM todo_item WHERE username=?");
                pstm.setObject(1, request.getAttribute("user"));
                ResultSet rst = pstm.executeQuery();
                List<TodoItemDTO> items = new ArrayList<>();
                while (rst.next()) {
                    items.add(new TodoItemDTO(rst.getInt("id"),
                            rst.getString("text"),
                            Priority.valueOf(rst.getString("priority")),
                            Status.valueOf(rst.getString("status")),
                            rst.getString("username")));
                }
                response.setContentType("application/json");
                response.getWriter().println(jsonb.toJson(items));

            } catch (SQLException throwables) {
                response.sendError(response.SC_INTERNAL_SERVER_ERROR);
                throwables.printStackTrace();
            }
        } else {
            int id = 0;
            try (Connection connection = cp.getConnection()) {
                id = Integer.parseInt(request.getPathInfo().replace("/", ""));
                request.setAttribute("user", "yohan");
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM todo_item WHERE id=? AND username=?");
                pstm.setObject(1, id);
                pstm.setObject(2, request.getAttribute("user"));
                ResultSet rst = pstm.executeQuery();
                if (!rst.next()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    response.setContentType("application/json");
                    TodoItemDTO item = new TodoItemDTO(rst.getInt("id"),
                            rst.getString("text"),
                            Priority.valueOf(rst.getString("priority")),
                            Status.valueOf(rst.getString("status")),
                            rst.getString("username"));
                    response.getWriter().println(jsonb.toJson(item));
                }

            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                e.printStackTrace();
            } catch (SQLException e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        try (Connection connection = cp.getConnection()) {
            int id = Integer.parseInt(req.getPathInfo().replace("/", ""));
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM todo_item WHERE id=? AND username=?");
            pstm.setObject(1, id);
            pstm.setObject(2, req.getAttribute("user"));
            ResultSet rst = pstm.executeQuery();
            if (!rst.next()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                pstm = connection.prepareStatement("DELETE * FROM todo_item WHERE id=? AND username=?");
                pstm.setObject(1, id);
                pstm.setObject(2, req.getAttribute("user"));
                Boolean success = pstm.executeUpdate() > 0;
                if (success) {
                    resp.sendError(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }

        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Jsonb jsonb = JsonbBuilder.create();
        try {
            TodoItemDTO item = jsonb.fromJson(req.getReader(), TodoItemDTO.class);
            if (item.getId() != null || item.getText() == null ||
                    item.getText().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
            try (Connection connection = cp.getConnection()) {
                int id = Integer.parseInt(req.getPathInfo().replace("/", ""));
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM `user` WHERE id=? AND username=?");
                pstm.setObject(1, id);
                pstm.setObject(2,req.getAttribute("user"));
                if (!pstm.executeQuery().next()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }else{
                   pstm = connection.prepareStatement("UPDATE todo_item SET text=?,priority=?,status=? WHERE id=?");
                   pstm.setObject(1,item.getText());
                   pstm.setObject(2,item.getPriority().toString());
                   pstm.setObject(3,item.getStatus().toString());
                   pstm.setObject(4,id);
                   boolean success= pstm.executeUpdate() > 0;
                   if (success){
                       resp.sendError(HttpServletResponse.SC_NO_CONTENT);
                   }else{
                       resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                   }
                }
            }
            }catch (SQLException throwables) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throwables.printStackTrace();
        }
    }
}
