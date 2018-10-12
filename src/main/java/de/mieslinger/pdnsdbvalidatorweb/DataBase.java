/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.mieslinger.pdnsdbvalidatorweb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Properties;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * @author mieslingert
 */
public class DataBase implements ServletContextListener {

    private static String jdbcUrl;
    private static String dbUser;
    private static String dbPass;
    private static String jdbcClass;
    private static final HikariDataSource ds;

    static {
        System.out.println("beginning static Initialized");
        // Do your thing during webapp's startup.
        Properties pvwProperties = new Properties();

        try {
            BufferedInputStream stream = new BufferedInputStream(new FileInputStream(System.getProperty("user.home") + "/.pdnsdbvalidator.properties"));
            pvwProperties.load(stream);
            stream.close();
        } catch (Exception ex) {
            System.out.println("could not load properties: " + ex.getMessage());
        }

        jdbcUrl = pvwProperties.getProperty("jdbcUrl", "jdbc:mysql://localhost:3306/pdns_test_db");
        dbUser = pvwProperties.getProperty("dbUser", "root");
        dbPass = pvwProperties.getProperty("dbPass", "");
        jdbcClass = pvwProperties.getProperty("jdbcClass", "com.mysql.jdbc.Driver");

        // Setup HikariCP
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);

        config.setUsername(dbUser);

        config.setPassword(dbPass);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useUnicode", "yes");
        config.addDataSourceProperty("characterEncoding", "utf8");
        config.setDriverClassName(jdbcClass);

        ds = new HikariDataSource(config);

        ds.setMaximumPoolSize(100);

        System.out.println("finished static Initialized");
    }

    public void contextInitialized(ServletContextEvent event) {
    }

    public void contextDestroyed(ServletContextEvent event) {
        // Do your thing during webapp's shutdown.
    }
}
