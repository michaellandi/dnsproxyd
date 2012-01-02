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

import java.util.*;

public class DNSResponse {
	/*
	 * Global variables.
	 */
	private byte[] 	_bRequest; //Pointer to the passed byte array.

	/*
	 * This is the default constructor for this class.
	 * It accepts a byte array request.
	 */
	public DNSResponse(byte[] request) {
		setRequest(request);
	}

//------------------------------------------------------------------------------

	/*
	 * Allows the user to set the DNS request so a proper response is created.
	 *
	 * USAGE: DNSResponse.setRequest(byteArrayResponse);
	 */
	public void setRequest(byte[] request) {
		_bRequest =request;
	}

	/*
	 * Return the request that this class is currently using.
	 */
	public byte[] getRequest() {
		return _bRequest;
	}

	/*
	 * Generate and return a response packet.
	 */
	public byte[] getResponse(byte[] bAddress) {
		return combine(genHeader(), genQuestion(), genAnswer(bAddress));
	}

	/*
	 * Returns the domain the request was querying for (eg. google.com).
	 */
	public String getDomain() {
		int intPosition = 12;
		int length = (int)_bRequest[intPosition];
		String strDomain = "";
		intPosition++;

		while (length != 0) {
			for (int i = intPosition; i < (intPosition + length); i++) {
				strDomain += (char)_bRequest[i];
			}

			intPosition += length;
			length = (int)_bRequest[intPosition];

			if (length != 0)
				strDomain += '.';
			intPosition++;
		}

		return strDomain;
	}

//------------------------------------------------------------------------------

	/*
	 * Generate header information.  Copy the ID from the exisitng header.
	 */
	private byte[] genHeader() {
		//The header is always 12 bytes (96 bits).
		byte[] bHeader = new byte[12];

		//Line 1 - Copy ID from header.
		for (int i = 0; i < 2; i++) {
			bHeader[i] = _bRequest[i];
		}

		//Line 2 - Packet options.
		bHeader[2] = (byte)129;	//10000001
		bHeader[3] = (byte)128;	//10000000

		//Line 3 - Questions
		bHeader[4] = 0;				//00000000
		bHeader[5] = 1;				//00000001

		//Line 4 - Answers
		bHeader[6] = 0;				//00000000
		bHeader[7] = 1;				//00000001
		
		//Line 5 - Nameservers
		bHeader[8] = 0;				//00000000
		bHeader[9] = 0;				//00000000

		//Line 6 - Arcount
		bHeader[10] = 0;			//00000000
		bHeader[11] = 0;			//00000000

		//Return the new byte array.
		return bHeader;
	}

	/*
	 * Generate the 'query' part of the packet.
	 */
	private byte[] genQuestion() {
		//This portion of the packet starts at byte 12.
		int intPosition = 12;
		//The first 'length' byte is at byte 12,0.
		int length = (int)_bRequest[intPosition];

		//Might be faster to 'look ahead' and determine how long domain name
		//is, but this works decently fast.
		Vector<Byte> vecDomain = new Vector<Byte>();
		//Add the length byte to the packet buffer.
		vecDomain.addElement((byte)length);
		//Move to the next position.
		intPosition++;

		//While the length is not 0, a zero length means no more data.
		while (length != 0) {
			for (int i = intPosition; i < intPosition + length; i++) {
				//Add each byte to the byte buffer.
				vecDomain.addElement(_bRequest[i]);
			}

			//Read the next length, and move to the new position.
			intPosition += length;
			length = (int)_bRequest[intPosition];
			vecDomain.addElement((byte)length);
			intPosition++;
		}

		//The next four bytes are important, so copy them from request array.
		vecDomain.addElement(_bRequest[intPosition + 0]);
		vecDomain.addElement(_bRequest[intPosition + 1]);
		vecDomain.addElement(_bRequest[intPosition + 2]);
		vecDomain.addElement(_bRequest[intPosition + 3]);

		//Create a byte array from the buffer vector.
		byte[] bQuestion = new byte[vecDomain.size()];
		for (int i = 0; i < vecDomain.size(); i++) {
			bQuestion[i] = vecDomain.get(i);
		}

		//Return the new byte array.
		return bQuestion;
	}

	/*
	 * Generate an answer as a byte array.
	 */
	private byte[] genAnswer(byte[] bAddress) {
		//The answer will always be 16 bytes.
		byte[] bAnswer = new byte[16];
		
		//Generate name.
		bAnswer[0] = (byte)192;	//11000000
		bAnswer[1] = 12;			//00001100
		
		//Type code (RR code).
		bAnswer[2] = 0;				//00000000
		bAnswer[3] = 1; 			//00000001

		//Class - Internet.
		bAnswer[4] = 0;				//00000000
		bAnswer[5] = 1;				//00000001

		//TTL
		bAnswer[6] = 0;				//00000000
		bAnswer[7] = 0;				//00000000
		bAnswer[8] = 0;				//00000000
		bAnswer[9] = (byte)151;  	//10010111

		//Length of the data section.
		bAnswer[10] = 0;			//00000000
		bAnswer[11] = 4;			//00000100

		//Data portion (the IP address).
		bAnswer[12] = bAddress[0];
		bAnswer[13] = bAddress[1];
		bAnswer[14] = bAddress[2];
		bAnswer[15] = bAddress[3];

		//Return new new byte array.
		return bAnswer;
	}

//------------------------------------------------------------------------------

	/*
	 * Used to combine three byte arrays.
	 */
	private byte[] combine(byte one[], byte two[], byte[] three)
	{
		byte[] bBuffer = new byte[one.length + two.length + three.length];

		System.arraycopy(one, 0, bBuffer, 0, one.length);
		System.arraycopy(two, 0, bBuffer, one.length, two.length);
		System.arraycopy(three, 0, bBuffer, (one.length + two.length),
			three.length);

		return (bBuffer);
	}
} //End of class DNSResponse.
