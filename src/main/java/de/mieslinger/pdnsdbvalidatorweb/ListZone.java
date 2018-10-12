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
@WebServlet(name = "ListZone", urlPatterns = {"/ListZone"})
public class ListZone extends HttpServlet {

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
            String hasSOA = "NO";
            String hasNS = "NO";
            String sOANameMatchesDomainName = "NO";

            response.setContentType("text/html;charset=UTF-8");

            // FIXME: check that there is a domainId
            Integer domainId = null;
            try {
                domainId = Integer.parseInt(request.getParameter("domainid"));
            } catch (Exception e) {
            }

            HikariDataSource ds = DataBase.getDs();
            Connection cn = ds.getConnection();

            // TODO: delete row from domainmetadata
            PreparedStatement stDomain = cn.prepareStatement("select d.name "
                    + "from domains d "
                    + "where d.id=?");
            stDomain.setInt(1, domainId);
            ResultSet rsD = stDomain.executeQuery();
            rsD.first();
            String domainname = rsD.getString(1);

            try {
                rsD.close();
            } catch (SQLException e) {
            }
            try {
                stDomain.close();
            } catch (SQLException e) {
            }

            out.println("<html>");
            out.println("<head>");
            out.println("<title>Zone " + domainname);
            out.println("</title>");
            out.println("</head>");
            out.println("<body>");

            out.println("<h1>Zone with potentially invalid records</h1>");

            out.println("<table border=\"1\" cellspacing=\"1\" cellpadding=\"1\" class=\"sortable\">"
                    + "<thead>"
                    + "<tr bgcolor=\"#CCCCCC\">"
                    + "<th>name</th>"
                    + "<th>ttl</th>"
                    + "<th>type</th>"
                    + "<th>content</th>"
                    + "<th>check result</th>"
                    + "<th>actions</th>"
                    + "</tr>"
                    + "</thead>"
                    + "<tbody>\n");

            PreparedStatement stZone = cn.prepareStatement("select r.id, r.name, r.ttl, r.type, r.prio, r.content "
                    + "from records r "
                    + "where r.domain_id=? "
                    + "limit 100");

            stZone.setInt(1, domainId);
            ResultSet rsZ = stZone.executeQuery();

            while (rsZ.next()) {
                ResourceRecord r = new ResourceRecord(rsZ.getString(2), rsZ.getLong(3), rsZ.getString(4), rsZ.getInt(5), rsZ.getString(6));

                if (hasSOA.equals("NO")) {
                    if (r.isSOA) {
                        hasSOA = "YES";
                    }
                }
                if (hasNS.equals("NO")) {
                    if (r.isNS) {
                        hasNS = "YES";
                    }
                }

                out.printf("<tr>"
                        + "<td>%s</td>"
                        + "<td>%d</td>"
                        + "<td>%s</td>"
                        + "<td>%s</td>"
                        + "<td>%s</td>"
                        + "<td>%s</td>"
                        + "</tr>\n",
                        r.getName(), r.getTtl(), r.getType(), r.getContent(), r.getRcMessage(), ""
                );
            }
            out.println("</tbody>"
                    + "</table>");
            out.println("<hr>");
            out.println("<h1>Zonelevel check</h1>");

            out.println("<p>Zone has SOA Record: " + hasSOA + "</p>");
            out.println("<p>Zone has NS Record: " + hasNS + "</p>");
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
                stZone.close();
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
