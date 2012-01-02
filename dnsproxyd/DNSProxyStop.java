/*
 * dnsproxyd
 * Version 1.0
 * Copyright © 2008 Michael Landi
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

import java.io.*;
import java.net.*;

public class DNSProxyStop {
	public static void main(String[] args) {
		byte[] bResponse = new byte[2];
		bResponse[0] = 24;
		bResponse[1] = 48;

		try {
			DatagramSocket dSocket = new DatagramSocket();
			InetAddress inaSender = InetAddress.getByName("127.0.0.1");
			DatagramPacket dPacket = new DatagramPacket(bResponse, 
				bResponse.length, inaSender, 4445);
			dSocket.send(dPacket);
		}
		catch (Exception e) {
			System.err.println(e);
		}
	}
}
