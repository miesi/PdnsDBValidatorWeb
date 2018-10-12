/*
$Id: $
 */
package de.mieslinger.pdnsdbvalidatorweb;

import java.net.InetAddress;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.SRVRecord;

/**
 *
 * @author mieslingert
 */
public class ResourceRecord {

    int rc = 1;
    String message = "FAILED";
    Record r = null;
    String name = null;
    Long ttl = null;
    String type = null;
    Integer prio = null;
    String content = null;

    private ResourceRecord() {
    }

    public ResourceRecord(String name, Long ttl, String type, Integer prio, String content) {
        this.name = name;
        this.ttl = ttl;
        this.type = type;
        this.prio = prio;
        this.content = content;

        try {
            switch (type) {
                case "NS":
                    Name ns = new Name(name + ".");
                    Name nsn = new Name(content + ".");
                    r = new NSRecord(ns, DClass.IN, ttl, nsn);
                    message = "OK";
                    rc = 0;
                    break;
                case "CNAME":
                    Name cn = new Name(name + ".");
                    Name ct = new Name(content + ".");
                    r = new CNAMERecord(cn, DClass.IN, ttl, ct);
                    message = "OK";
                    rc = 0;
                    break;
                case "PTR":
                    Name ptr = new Name(name + ".");
                    Name ptrdname = new Name(content + ".");
                    r = new PTRRecord(ptr, DClass.IN, ttl, ptrdname);
                    message = "OK";
                    rc = 0;
                    break;
                case "MX":
                    Name mxn = new Name(name + ".");
                    Name mxt = new Name(content + ".");
                    r = new MXRecord(mxn, DClass.IN, ttl, prio, mxt);
                    message = "OK";
                    rc = 0;
                    break;
                case "SOA":
                    Name soan = new Name(name + ".");
                    String[] soaFields = content.split(" ");
                    Name pri = new Name(soaFields[0]);
                    Name mail = new Name(soaFields[1]);
                    Long serial = Long.parseLong(soaFields[2]);
                    Long refresh = Long.parseLong(soaFields[3]);
                    Long retry = Long.parseLong(soaFields[4]);
                    Long expire = Long.parseLong(soaFields[5]);
                    Long minimum = Long.parseLong(soaFields[6]);
                    r = new SOARecord(soan, DClass.IN, ttl, pri, mail, serial, refresh, retry, expire, minimum);
                    message = "OK";
                    rc = 0;
                    break;
                case "A":
                    Name an = new Name(name + ".");
                    InetAddress ip4 = InetAddress.getByName(content);
                    r = new ARecord(an, DClass.IN, ttl, ip4);
                    message = "OK";
                    rc = 0;
                    break;
                case "AAAA":
                    Name aaaan = new Name(name + ".");
                    InetAddress ip6 = InetAddress.getByName(content);
                    r = new AAAARecord(aaaan, DClass.IN, ttl, ip6);
                    message = "OK";
                    rc = 0;
                    break;
                case "SRV":
                    Name srvn = new Name(name + ".");
                    String[] srvFields = content.split(" ");
                    Integer srvprio = Integer.parseInt(srvFields[0]);
                    Integer weight = Integer.parseInt(srvFields[1]);
                    Integer port = Integer.parseInt(srvFields[2]);
                    Name host = new Name(srvFields[3]);
                    r = new SRVRecord(srvn, DClass.IN, ttl, srvprio, weight, port, host);
                    message = "OK";
                    rc = 0;
                    break;

                default:
                    message = "Record type not supported";
                    break;
            }
        } catch (Exception e) {
            message = "FAILED: " + e.toString();
        }
    }

    public int getRc() {
        return rc;
    }

    public String getMessage() {
        return message;
    }

    public String getRcMessage() {
        return "" + rc + " " + message;
    }

    public String getName() {
        return name;
    }

    public Long getTtl() {
        return ttl;
    }

    public String getType() {
        return type;
    }

    public Integer getPrio() {
        return prio;
    }

    public String getContent() {
        return content;
    }

}
