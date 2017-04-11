/*
 * dnsproxyd
 * Version 1.1
 * Class created 2017 by Thomas Schuett (Munich)
 * 
 * inspired by dig.java and lookup.java from dnsjava module
 *
 * This file is part of dnsproxyd.
 *
 * Dnsproxyd is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dnsproxyd is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dnsproxyd.  If not, see <http://www.gnu.org/licenses/>
 */

import java.io.IOException;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;


public class DNSQuery_dnsjava {

static Name name = null;
static int type = Type.A, dclass = DClass.IN;

// test method
public static void main(String argv[]) throws IOException {

	String nameserver = "192.168.0.1";
	String hostname = "www.ibm.com";

	String response = doLookup(nameserver, hostname);
	System.out.println(response);
}

public static String doLookup(String nameserver, String hostname) throws IOException {
	Message query, response;
	Record rec;
	SimpleResolver res = null;
	boolean printQuery = false;

	res = new SimpleResolver(nameserver);
	name = Name.fromString(hostname, Name.root);

	rec = Record.newRecord(name, type, dclass);
	query = Message.newQuery(rec);
	if (printQuery)
		System.out.println(query);
	response = res.send(query);

	int rcode = response.getHeader().getRcode();
	if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN) {
		// The server we contacted is broken or otherwise unhelpful.
		//badresponse_error = Rcode.string(rcode);
		return null;
	}

	if (response.isSigned() && ! response.isVerified() ) {
			return null;
	}

	String[] responseLines =  response.sectionToString(1).split("\n");
	
	// first try to find a IPv4 address
	for (String line: responseLines) {
		int idx = line.indexOf("IN\tA\t"); 
		if ( idx > -1) {
			return line.substring(idx+5).trim();
		}
	}

	// then try to find a IPv6 address
	for (String line: responseLines) {
		int idx = line.indexOf("IN\tAAAA\t"); 
		if ( idx > -1) {
			return line.substring(idx+8).trim();
		}
	}

	return null;
}

}
