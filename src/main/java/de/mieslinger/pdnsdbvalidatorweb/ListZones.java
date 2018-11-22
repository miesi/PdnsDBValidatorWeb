/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.mieslinger.pdnsdbvalidatorweb;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author mieslingert
 */
@WebServlet(name = "ListZones", urlPatterns = {"/ListZones"})
public class ListZones extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();

        try {
            long startTs = System.currentTimeMillis();
            response.setContentType("text/html;charset=UTF-8");

            out.println("<html>");
            out.println("<head>");
            out.println("<title>Dubious Zones");
            out.println("</title>");
            out.println("</head>");
            out.println("<body>");

            HikariDataSource ds = DataBase.getDs();
            Connection cn = ds.getConnection();

            PreparedStatement stZones = cn.prepareStatement("select d.id, d.name, dm.content "
                    + "from domainmetadata dm, domains d "
                    + "where d.id=dm.domain_id "
                    + "  and dm.kind='broken' "
                    + "limit 100");

            ResultSet rsZ = stZones.executeQuery();

            out.println("<h1>Zones with potentially invalid records</h1>");

            out.println("<table border=\"1\" cellspacing=\"1\" cellpadding=\"1\" class=\"sortable\">"
                    + "<thead>"
                    + "<tr bgcolor=\"#CCCCCC\">"
                    + "<th>id</th>"
                    + "<th>Zone</th>"
                    + "<th>Reason</th>"
                    + "</tr>"
                    + "</thead>"
                    + "<tbody>\n");

            while (rsZ.next()) {
                out.printf("<tr>"
                        + "<td><a href=\"ListZone?domainid=%d\">%d</a></td>"
                        + "<td><a href=\"ListZone?domainid=%d\">%s</a></td>"
                        + "<td>%s</td>"
                        + "</tr>\n",
                        rsZ.getInt(1), rsZ.getInt(1),
                        rsZ.getInt(1), rsZ.getString(2),
                        rsZ.getString(3)
                );
            }
            out.println("</tbody>"
                    + "</table>");
            out.println("<hr>");

            out.println("Session und Connection Information:<br>");
            out.println("RemoteAddress: " + request.getRemoteAddr());
            HttpSession s = request.getSession(false);

            out.println("<hr>");
            out.println("Generated at: " + new Date().toString() + "<br>");
            out.println("Total generation time: " + (System.currentTimeMillis() - startTs) + "ms<br>");

            out.println("</body>");
            out.println("</html>");
            try {
                rsZ.close();
            } catch (SQLException e) {
            }
            try {
                stZones.close();
            } catch (SQLException e) {
            }
            try {
                cn.close();
            } catch (SQLException e) {
            }
        } catch (Exception e) {
            out.println(e.toString());
        } finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
