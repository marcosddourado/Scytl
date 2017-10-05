import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Client {
	private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

	public static void main(String[] args) throws UnknownHostException, IOException {
		String host = "189.85.81.90";
		int port = 50021;
		Socket client = new Socket(host, port);

		HashMap<String, String> inOutTable = new HashMap<String, String>();
		inOutTable.put("0000", "11110");
		inOutTable.put("0001", "01001");
		inOutTable.put("0010", "10100");
		inOutTable.put("0011", "10101");
		inOutTable.put("0100", "01010");
		inOutTable.put("0101", "01011");
		inOutTable.put("0110", "01110");
		inOutTable.put("0111", "01111");
		inOutTable.put("1000", "10010");
		inOutTable.put("1001", "10011");
		inOutTable.put("1010", "10110");
		inOutTable.put("1011", "10111");
		inOutTable.put("1100", "11010");
		inOutTable.put("1101", "11011");
		inOutTable.put("1110", "11100");
		inOutTable.put("1111", "11101");

		System.out.println("Connection established!");

		InputStream in = client.getInputStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] read = new byte[1024];
		int len;
		while ((len = in.read(read)) > -1) {
			baos.write(read, 0, len);
		}

		// this is the final byte array which contains the data
		// read from Socket
		byte[] bytes = baos.toByteArray();

		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X ", b));
		}
		System.out.println(sb.toString());// here sb is hexadecimal string

		String[] hexadecimalBlocks = sb.toString().split(" ");
		ArrayList<String> binaryPackets = new ArrayList<String>();
		String packet = "";

		for (String block : hexadecimalBlocks) {
			// System.out.print(hexToBin(block)+" ");

			if (block.equalsIgnoreCase("C6")) {
				// System.out.println("\nStart of a packet detected");
			} else if (block.equalsIgnoreCase("6B") || block.equalsIgnoreCase("21") && packet != "") {
				// System.out.println("\nEnd of a packet detected");
				binaryPackets.add(packet);
				packet = "";
			} else {
				String binaryBlock = hexToBin(block);

				int addZeros = 8 - binaryBlock.length();
				for (int i = 0; i < addZeros; i++) {
					binaryBlock = "0" + binaryBlock;
				}

				packet += binaryBlock;
			}
		}

		int amountOfpackets = binaryPackets.size();
		System.out.println("amount of packets: " + amountOfpackets);
		String decodedMessage = "";
		// group in 5-bit blocks
		for (String binary : binaryPackets) {
			System.out.println("packet: \n" + binary + ", size: " + binary.length());

			String fiveBitBlocks = "";
			for (int i = 0; i < binary.length(); i++) {
				if ((i + 1) % 5 != 0) {
					fiveBitBlocks += binary.charAt(i);
				} else {
					fiveBitBlocks += binary.charAt(i) + " ";
				}
			}

			System.out.println(fiveBitBlocks);

			String[] blocks = fiveBitBlocks.split(" ");
			String decodedBits = "";
			for (int i = 0; i < blocks.length; i++) {
				if ((i + 1) % 2 == 0) {
					decodedBits += getKeyByValue(inOutTable, blocks[i]) + " ";
				} else {
					decodedBits += getKeyByValue(inOutTable, blocks[i]);
				}
			}
			System.out.println(decodedBits);
			String[] decodedParts = decodedBits.split(" ");
			String hexadecimalString = "";
			for (int i = 0; i < decodedParts.length; i++) {
				if (i != 0) {
					hexadecimalString += " ";
				}
				hexadecimalString += Long.toHexString(Long.parseLong(decodedParts[i], 2)) + " ";
			}

			System.out.println(hexadecimalString);

			String[] hexadecimalParts = hexadecimalString.split(" ");
			for (String part : hexadecimalParts) {
				decodedMessage += hexToAscii(part);
			}
		}
		System.out.println("decodedMessage: " + decodedMessage);

		String invertedMessage = new StringBuilder(decodedMessage).reverse().toString();
		if (invertedMessage.startsWith(" ")) {
			System.out.println("inverted message starts with a space");
			invertedMessage = invertedMessage.substring(1);
			invertedMessage += " ";
		}

		System.out.println("invertedMessage: " + invertedMessage);
		System.out.println("Hex value is " + stringToHex(invertedMessage));

		String invertedHexMessage = stringToHex(invertedMessage);
		ArrayList<String> invertedHexadecimalPackets = new ArrayList<String>();
		String buffer = "";
		for (int i = 0; i < invertedHexMessage.length(); i++) {
			buffer += invertedHexMessage.charAt(i);
			if ((i + 1) % 2 == 0) {
				buffer += " ";
			}
			if ((i + 1) % 8 == 0) {
				invertedHexadecimalPackets.add(buffer);
				buffer = "";
			}
		}

		ArrayList<String> packetsToSend = new ArrayList<String>();
		System.out.println("invertedHexadecimalPackets: ");
		for (String hexPack : invertedHexadecimalPackets) {
			System.out.println(hexPack);
			binaryPackets = new ArrayList<String>();
			packet = "";
			String[] hexBlocks = hexPack.split(" ");

			for (String block : hexBlocks) {
				String binaryBlock = hexToBin(block);

				int addZeros = 8 - binaryBlock.length();
				for (int i = 0; i < addZeros; i++) {
					binaryBlock = "0" + binaryBlock;
				}

				packet += binaryBlock;
			}
			binaryPackets.add(packet);

			for (String binary : binaryPackets) {
				String fourBitBlocks = "";
				for (int i = 0; i < binary.length(); i++) {
					if ((i + 1) % 4 != 0) {
						fourBitBlocks += binary.charAt(i);
					} else {
						fourBitBlocks += binary.charAt(i) + " ";
					}
				}
				System.out.println(fourBitBlocks);
				String[] blocks = fourBitBlocks.split(" ");
				String codedBits = "";
				for (int i = 0; i < blocks.length; i++) {
					codedBits += inOutTable.get(blocks[i]);
				}

				String eightBitsPacket = "";
				for (int i = 0; i < codedBits.length(); i++) {
					if (i % 8 == 0 && i != 0) {
						eightBitsPacket += " ";
					}
					eightBitsPacket += codedBits.charAt(i);
				}
				System.out.println(eightBitsPacket);

				blocks = eightBitsPacket.split(" ");
				packet = "";
				for (String block : blocks) {
					packet += binToHex(block) + " ";
				}
				packetsToSend.add(packet);
				System.out.println(packet);
			}
		}
		String finalPacket = "";
		for (int i = 0; i < packetsToSend.size(); i++) {
			finalPacket += "C6 " + packetsToSend.get(i);

			if ((i + 1) == packetsToSend.size()) {
				finalPacket += "21";
			} else {
				finalPacket += "6B ";
			}
		}
		
		System.out.println("final packet: "+finalPacket.toUpperCase());
	}

	public static String stringToHex(String input) throws UnsupportedEncodingException {
		if (input == null)
			throw new NullPointerException();
		return asHex(input.getBytes());
	}

	private static String asHex(byte[] buf) {
		char[] chars = new char[2 * buf.length];
		for (int i = 0; i < buf.length; ++i) {
			chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
			chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
		}
		return new String(chars);
	}

	static String hexToBin(String s) {
		return new BigInteger(s, 16).toString(2);
	}

	static String binToHex(String s) {
		return new BigInteger(s, 2).toString(16);
	}

	private static String hexToAscii(String hexStr) {
		StringBuilder output = new StringBuilder("");

		for (int i = 0; i < hexStr.length(); i += 2) {
			String str = hexStr.substring(i, i + 2);
			output.append((char) Integer.parseInt(str, 16));
		}

		return output.toString();
	}

	public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
		for (java.util.Map.Entry<T, E> entry : map.entrySet()) {
			if (Objects.equals(value, entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}
}