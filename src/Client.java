import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Client {
	private static final String END_TRANSMISSION = "21";
	private static final String END_PKT = "6B";
	private static final String START_PKT = "C6";
	private static final String ERR = "0xC6 0x52 0xD7 0x45 0xD2 0x9E 0x21";
	private static final String OK = "0xC6 0x57 0x55 0x7A 0x7A 0x9E 0x21";
	private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
	private static final String HOST = "189.85.81.90";
	private static final int PORT = 50031;

	public static void main(String[] args) throws UnknownHostException, IOException {
		Socket client = new Socket(HOST, PORT);
		PrintStream request = new PrintStream(client.getOutputStream());

		HashMap<String, String> inOutTable = new HashMap<String, String>();
		initBinaryHashMap(inOutTable);

		System.out.println("Connection established!");

		InputStream in = client.getInputStream();

		while (true) {
			String hexadecimalMessage = receiveHexadecimalMessage(in);

			if (hexadecimalMessage.equalsIgnoreCase(OK)) {
				System.out.println("Server response: OK");
				break;
			} else if (hexadecimalMessage.equalsIgnoreCase(ERR)) {
				System.out.println("Server response: ERR");
				break;
			} else if(hexadecimalMessage.equalsIgnoreCase("")) {
				System.out.println("Nothing received from the server as return!");
				break;
			} else {
				String[] hexadecimalBlocks = hexadecimalMessage.split(" ");
				ArrayList<String> binaryPackets = new ArrayList<String>();
				
				String packet = hexPacketToBinary(hexadecimalBlocks, binaryPackets);
				
				System.out.println("Hexadecimal message: " + hexadecimalMessage);

				String decodedMessage = decodeMessage(inOutTable, binaryPackets);

				System.out.println("Decoded message: " + decodedMessage);

				String invertedMessage = invertMessage(decodedMessage);

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
				for (String hexPack : invertedHexadecimalPackets) {
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
						String fourBitBlocks = dividePacketInBlocks(binary, 4);
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

						blocks = eightBitsPacket.split(" ");
						packet = "";
						for (String block : blocks) {
							packet += binToHex(block) + " ";
						}
						packetsToSend.add(packet);
					}
				}
				String finalPacket = "";
				for (int i = 0; i < packetsToSend.size(); i++) {
					finalPacket += "C6 " + packetsToSend.get(i);

					if ((i + 1) == packetsToSend.size()) {
						finalPacket += END_TRANSMISSION;
					} else {
						finalPacket += "6B ";
					}
				}

				System.out.println("final packet: " + finalPacket.toUpperCase());

				request.println(finalPacket.toUpperCase());
			}
		}
	}

	private static String invertMessage(String decodedMessage) {
		String invertedMessage = new StringBuilder(decodedMessage).reverse().toString();
		if (invertedMessage.startsWith(" ")) {
			invertedMessage = invertedMessage.substring(1);
			invertedMessage += " ";
		}
		return invertedMessage;
	}

	private static String decodeMessage(HashMap<String, String> inOutTable, ArrayList<String> binaryPackets) {
		String decodedMessage = "";
		
		// group in 5 bit blocks
		for (String binary : binaryPackets) {
			String fiveBitBlocks = dividePacketInBlocks(binary, 5);
			String[] blocks = fiveBitBlocks.split(" ");
			String decodedBits = "";

			// transform 5 bit blocks into 8 bit blocks
			for (int i = 0; i < blocks.length; i++) {
				if ((i + 1) % 2 == 0) {
					decodedBits += getKeyByValue(inOutTable, blocks[i]) + " ";
				} else {
					decodedBits += getKeyByValue(inOutTable, blocks[i]);
				}
			}
			
			//transform 8 bit blocks into a hexadecimal String
			String[] decodedParts = decodedBits.split(" ");
			String hexadecimalString = "";
			for (int i = 0; i < decodedParts.length; i++) {
				if (i != 0) {
					hexadecimalString += " ";
				}
				hexadecimalString += Long.toHexString(Long.parseLong(decodedParts[i], 2)) + " ";
			}

			//decode the message
			String[] hexadecimalParts = hexadecimalString.split(" ");
			for (String part : hexadecimalParts) {
				decodedMessage += hexToAscii(part);
			}
		}
		
		return decodedMessage;
	}

	private static String dividePacketInBlocks(String binary, int blockSize) {
		String bitBlocks = "";
		for (int i = 0; i < binary.length(); i++) {
			if ((i + 1) % blockSize != 0) {
				bitBlocks += binary.charAt(i);
			} else {
				bitBlocks += binary.charAt(i) + " ";
			}
		}
		return bitBlocks;
	}

	private static String hexPacketToBinary(String[] hexadecimalBlocks, ArrayList<String> binaryPackets) {
		String packet = "";
		
		for (String block : hexadecimalBlocks) {
			if (block.equalsIgnoreCase(START_PKT)) {
				// Nothing to do
			} else if (block.equalsIgnoreCase(END_PKT) || block.equalsIgnoreCase(END_TRANSMISSION) && packet != "") {
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
		return packet;
	}

	private static void initBinaryHashMap(HashMap<String, String> inOutTable) {
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
	}

	public static String stringToHex(String input) throws UnsupportedEncodingException {
		if (input == null)
			throw new NullPointerException();
		return asHex(input.getBytes());
	}

	private static String receiveHexadecimalMessage(InputStream in) throws IOException {
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

		return sb.toString();
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