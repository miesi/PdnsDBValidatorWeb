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
            String dnsName = "";

            response.setContentType("text/html;charset=UTF-8");

            // FIXME: fail gracefully without domainId
            Long domainId = null;
            try {
                domainId = Long.parseLong(request.getParameter("domainid"));
            } catch (Exception e) {
            }

            HikariDataSource ds = DataBase.getDs();
            Connection cn = ds.getConnection();

            // Assume Zone will be fixed
            PreparedStatement delDM = cn.prepareStatement("delete from domainmetadata where domain_id=? and kind='broken'");
            delDM.setLong(1, domainId);
            delDM.execute();
            delDM.close();

            // Get next domainId for link to next zone to be fixed
            PreparedStatement stNextDomain = cn.prepareStatement("select domain_id "
                    + "from domainmetadata "
                    + "where domain_id != ? "
                    + "limit 1");
            stNextDomain.setLong(1, domainId);
            ResultSet rsDN = stNextDomain.executeQuery();
            rsDN.first();
            Long nextDomId = rsDN.getLong(1);
            rsDN.close();

            // handle delete zone
            // overwrite now invalid domainId with a valid domainId
            String actionDeleteZone = request.getParameter("actionDeleteZone");
            if (actionDeleteZone != null && actionDeleteZone.equals("DeleteZone")) {
                Long dDomainId = Long.parseLong(request.getParameter("domainid"));
                PreparedStatement delZ = cn.prepareStatement("delete from records where domain_id=?");
                delZ.setLong(1, dDomainId);
                delZ.execute();
                delZ.close();
                delZ = cn.prepareStatement("delete from domains where id=?");
                delZ.setLong(1, dDomainId);
                delZ.execute();
                delZ.close();
                // switch to next domain
                domainId = nextDomId;
            }

            // get data from domains table for domainId
            PreparedStatement stDomain = cn.prepareStatement("select d.name, d.type "
                    + "from domains d "
                    + "where d.id=?");
            stDomain.setLong(1, domainId);
            ResultSet rsD = stDomain.executeQuery();
            rsD.first();
            String domainName = rsD.getString(1);
            String domainType = rsD.getString(2);

            try {
                rsD.close();
            } catch (SQLException e) {
            }
            try {
                stDomain.close();
            } catch (SQLException e) {
            }

            // handle generate SOA
            // use provided NS Name from cgi.
            String actionGenerateSOA = request.getParameter("actionGenerateSOA");
            if (actionGenerateSOA != null && actionGenerateSOA.equals("GenerateSOA")) {
                Long gDomainId = Long.parseLong(request.getParameter("domainid"));
                String gDnsName = request.getParameter("dnsname");

                PreparedStatement insSOA = cn.prepareStatement("insert into records(domain_id, name, type, content, ttl) "
                        + "values (?, ?, 'SOA', ?, 86400)");

                insSOA.setLong(1, gDomainId);
                insSOA.setString(2, domainName);
                insSOA.setString(3, String.format("%s %s 1 28800 7200 604800 600", gDnsName, DataBase.getDefaultSoaMail()));

                insSOA.execute();
                insSOA.close();
            }

            // handle delete Record
            String actionDeleteRecord = request.getParameter("actionDeleteRecord");
            if (actionDeleteRecord != null && actionDeleteRecord.equals("Delete")) {
                Long recordId = Long.parseLong(request.getParameter("recordid"));
                PreparedStatement delR = cn.prepareStatement("delete from records where id=?");
                delR.setLong(1, recordId);
                delR.execute();
                delR.close();
            }

            // handle update record
            String actionUpdateRecord = request.getParameter("actionUpdateRecord");
            if (actionUpdateRecord != null && actionUpdateRecord.equals("Update")) {
                Long recordId = Long.parseLong(request.getParameter("recordid"));
                String content = request.getParameter("content");
                PreparedStatement updR = cn.prepareStatement("update records set content=? where id=?");
                updR.setString(1, content);
                updR.setLong(2, recordId);
                updR.execute();
                updR.close();
            }

            // output the usual html
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Zone " + domainName);
            out.println("</title>");
            out.println("</head>");
            out.println("<body>");

            out.println("<h1>Zone with potentially invalid records</h1>");

            out.printf("<p><a href=\"ListZone?domainid=%d\">Go to next Zone</a></p>", nextDomId);

            out.println("<h2>domains table content</h2>");
            out.println("domainId: " + domainId + "<br>domainName: " + domainName + "<br>domainType: " + domainType);

            out.println("<h2>records table content</h2>");
            out.println("<table border=\"1\" cellspacing=\"1\" cellpadding=\"1\">"
                    + "<thead>"
                    + "<tr bgcolor=\"#CCCCCC\">"
                    + "<th>name</th>"
                    + "<th>ttl</th>"
                    + "<th>type</th>"
                    + "<th>prio</th>"
                    + "<th>content</th>"
                    + "<th>check result</th>"
                    + "<th>actions</th>"
                    + "</tr>"
                    + "</thead>"
                    + "<tbody>\n");

            // get zone content
            // FIXME: handle long zone clever
            PreparedStatement stZone = cn.prepareStatement("select r.id, r.name, r.ttl, r.type, r.prio, r.content "
                    + "from records r "
                    + "where r.domain_id=? ");

            stZone.setLong(1, domainId);
            ResultSet rsZ = stZone.executeQuery();

            while (rsZ.next()) {
                ResourceRecord r = new ResourceRecord(rsZ.getString(2), rsZ.getLong(3), rsZ.getString(4), rsZ.getInt(5), rsZ.getString(6));

                if (hasSOA.equals("NO")) {
                    if (r.isSOA()) {
                        hasSOA = "YES";
                        if (r.getName().equals(domainName)) {
                            sOANameMatchesDomainName = "YES";
                        }
                    }
                }
                if (hasNS.equals("NO")) {
                    if (r.isNS()) {
                        hasNS = "YES";
                        dnsName = r.getContent();
                    }
                }

                if (r.getRc() != 0) {
                    String deleteRecordButton = String.format("<form name=\"deleteRecord\" method=\"post\">"
                            + "<input type=\"hidden\" name=\"recordid\" value=\"%d\">"
                            + "<input type=\"submit\" name=\"actionDeleteRecord\" value=\"Delete\">"
                            + "</form>", rsZ.getLong(1));

                    String editButton = String.format("<form name=\"updateRecord\" method=\"post\">"
                            + "<input type=\"hidden\" name=\"recordid\" value=\"%d\">"
                            + "<textarea name=\"content\" cols=\"110\" rows=\"8\">%s</textarea>"
                            + "<input type=\"submit\" name=\"actionUpdateRecord\" value=\"Update\">"
                            + "</form>", rsZ.getLong(1), r.getContent());

                    out.printf("<tr>"
                            + "<td>%s</td>"
                            + "<td>%d</td>"
                            + "<td>%s</td>"
                            + "<td>%d</td>"
                            + "<td>%s</td>"
                            + "<td>%s</td>"
                            + "<td>%s</td>"
                            + "</tr>\n",
                            r.getName(), r.getTtl(), r.getType(), r.getPrio(), editButton, r.getRcMessage(), deleteRecordButton
                    );
                } else {
                    out.printf("<tr>"
                            + "<td>%s</td>"
                            + "<td>%d</td>"
                            + "<td>%s</td>"
                            + "<td>%d</td>"
                            + "<td>%s</td>"
                            + "<td>%s</td>"
                            + "<td>%s</td>"
                            + "</tr>\n",
                            r.getName(), r.getTtl(), r.getType(), r.getPrio(), r.getContent(), r.getRcMessage(), ""
                    );
                }
            }
            out.println("</tbody>"
                    + "</table>");

            out.printf("<form name=\"deleteZone\" method=\"post\">"
                    + "<input type=\"hidden\" name=\"domainid\" value=\"%d\">"
                    + "<input type=\"submit\" name=\"actionDeleteZone\" value=\"DeleteZone\">"
                    + "</form>", domainId);

            if (hasNS.equals("YES") && hasSOA.equals("NO")) {
                out.printf("<form name=\"generateSOA\" method=\"post\">"
                        + "<input type=\"hidden\" name=\"domainid\" value=\"%d\">"
                        + "<input type=\"hidden\" name=\"dnsname\" value=\"%s\">"
                        + "<input type=\"submit\" name=\"actionGenerateSOA\" value=\"GenerateSOA\">"
                        + "</form>", domainId, dnsName);
            }

            out.println("<hr>");
            out.println("<h1>Zonelevel check</h1>");

            out.println("<p>Zone has SOA Record: " + hasSOA + "</p>");
            out.println("<p>Name of SOA Record matches domains table: " + sOANameMatchesDomainName + "</p>");

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
