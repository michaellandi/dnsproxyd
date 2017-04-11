/*
 * dnsproxyd
 * Version 1.1
 * Class created 2017 by Thomas Schuett (Munich)
 *
 * This query implementation is in status experimental.
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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DNSQuery_passThrough {
	public static byte[] doLookup(byte[] request) throws Exception {
		byte[] nameserver = new byte[4];

		nameserver = new byte[]{(byte) 192, (byte) 168, 0 ,1};
		InetAddress dnsServer = InetAddress.getByAddress(nameserver);
		
        DatagramPacket dPacket = new DatagramPacket(request, 
        		request.length, dnsServer, 53);
        
        DatagramSocket datagramSocket = new DatagramSocket();
	    datagramSocket.send(dPacket);

		byte[] bResponse = new byte[96];
		dPacket = new DatagramPacket(bResponse, bResponse.length);
		datagramSocket.receive(dPacket);

		datagramSocket.close();

		return bResponse;
	}

}
