import java.util.*;
import java.lang.*;
import java.io.*;

class Main {
	static int boardWidth = 10;
	static int boardHeight = 10;
	static boolean canPlaceAdjacentShips = false;
	static boolean canPlaceDiagonalShips = false;
	//number of ships for each length. 1 based.
	static int[] numShips = {4,3,2,1,0};
	static boolean alphanumericCoords = true;
	static boolean showProbabilityBoard = false;
	
  	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);

		//optional custom initialization
		out("do you want to use the standard battleship setup? (y/n): ");
		if(!scan.next().equals("y")) {
			out("board size: ");
			int size = scan.nextInt();
			boardWidth = size;
			boardHeight = size;
			
			out("can ships be placed next to each other? (y/n) ");
			canPlaceAdjacentShips = scan.next().equals("y");
			
			out("can ships be placed diagonally from each other? (y/n) ");
			canPlaceDiagonalShips = scan.next().equals("y");
			
			for(int i = 0; i < 5; i++) {
				out("# of length " + (i+1) + " ships: ");
				numShips[i] = scan.nextInt();
			}

			out("should the coordinates be formatted alphanumerically? (y/n): ");
			alphanumericCoords = scan.next().equals("y");

			out("do you want to show the probability distribution? (advanced) (y/n): ");
			showProbabilityBoard = scan.next().equals("y");
		}

		//set board layout ('.' = unknown, 'x' = ship, 'o' = not a ship)
		char[][] board = new char[boardHeight][boardWidth];
		fill(board, '.');

		//while all ships have not sunk, play the game
		while(!isAllZero(numShips)) {
			//print state of board
			printBoard(board);
			
			int[] move = makeMove(board);

			int madeX, madeY;
			if(alphanumericCoords) {
				out("make move " + coordinateToAlphanumeric(move[0], move[1]));
				out("what move did you make? (do not include spaces) ");
				int[] inputMadeMove = alphanumericToCoordinate(scan.next());
				madeX = inputMadeMove[1] + 1;
				madeY = inputMadeMove[0] + 1;
			} else {
				out("make move x=" + (move[1]+1) + " y=" + (move[0]+1));
				out("what move did you make? (x): ");
				madeX = scan.nextInt();
				out("what move did you make? (y): ");
				madeY = scan.nextInt();
			}
			int[] madeMove = {madeY-1, madeX-1};			
			out("is this a hit or miss (h/m): ");
			char newChar = scan.next().equals("h") ? 'x' : 'o';
			board[madeMove[0]][madeMove[1]] = newChar;

			//update number of ships remaining if necessary
			if(newChar == 'x') {
				out("did this hit sink the ship (y/n): ");
				boolean sunk = scan.next().equals("y");
				if(sunk) {
					out("how long was the ship that sank? ");
					int length = scan.nextInt();
					numShips[length-1]--;
					//update surrounding squares with corresponding not a ship and ship values
					updateSquares(board, madeMove[0], madeMove[1], length, sunk);
				} else {
					updateSquares(board, madeMove[0], madeMove[1], -1, sunk);
				}
			}
		}
		out("you win!");
		

		scan.close();
  	}

	public static String coordinateToAlphanumeric(int y, int x) {
		//take in 0 based coords
		return String.valueOf((char)('A'+x)) + (y+1);
	}

	public static int[] alphanumericToCoordinate(String xy) {
		//return [y, x] as 0 based
		int x = Character.toUpperCase(xy.charAt(0)) - 'A';
		int y = Integer.parseInt(xy.substring(1)) - 1;
		return new int[] {y, x};
	}

	public static void updateSquares(char[][] board, int y, int x, int length, boolean sunk) {
		if(length == 0) {
			return;
		}
		if(sunk) {
			if(!canPlaceAdjacentShips) {
				//mark off adjacent squares
				if(x != 0) {
					//do left
					if(board[y][x-1] == 'x') {
						//recurse
						updateSquares(board, y, x-1, length-1, sunk);
					} else {
						board[y][x-1] = 'o';
					}
				}
				if(x != board[0].length-1) {
					//do right
					if(board[y][x+1] == 'x') {
						//recurse
						updateSquares(board, y, x+1, length-1, sunk);
					} else {
						board[y][x+1] = 'o';
					}
				}
				if(y != 0) {
					//do up
					if(board[y-1][x] == 'x') {
						//recurse
						updateSquares(board, y-1, x, length-1, sunk);
					} else {
						board[y-1][x] = 'o';
					}
				}
				if(y != board.length-1) {
					//do down
					if(board[y+1][x] == 'x') {
						//recurse
						updateSquares(board, y+1, x, length-1, sunk);
					} else {
						board[y+1][x] = 'o';
					}
				}
			}
		}
		if(!canPlaceDiagonalShips) {
			//mark off diagonal squares
			if(x != 0 && y != 0) {
				//do top left corner
				if(board[y-1][x-1] != 'x') {
					board[y-1][x-1] = 'o';
				}
			}
			if(x != 0 && y != board.length-1) {
				//do bottom left corner
				if(board[y+1][x-1] != 'x') {
					board[y+1][x-1] = 'o';
				}
			}
			if(x != board[0].length - 1 && y != 0) {
				//do top right corner
				if(board[y-1][x+1] != 'x') {
					board[y-1][x+1] = 'o';
				}
			}
			if(x != board[0].length - 1 && y != board.length - 1) {
				//do bottom right corner
				if(board[y+1][x+1] != 'x') {
					board[y+1][x+1] = 'o';
				}
			}
		}
	}

	public static int[] makeMove(char[][] board) {
		//return y and x coordinates of move (0 indexed)
		//y,x
		int[] move = new int[2];

		//calculate and sum all probability tables for all ship lengths
		int[][] probabilities = new int[board.length][board[0].length];
		for(int n = 0; n < numShips.length; n++) {
			if(numShips[n] == 0) {
				continue;
			}
			
			int shipLength = (n+1);
			//check for horizontal ships
			for(int i = 0; i < board.length; i++) {
				for(int j = 0; j < board[0].length - shipLength + 1; j++) {
					//check if [i,j] to [i,j+shipLength-1] inclusive is free for a ship
					boolean free = true;
					for(int k = j; k <= j + shipLength - 1; k++) {
						if(board[i][k] == 'o') {
							free = false;
						}
					}
					if(free) {
						//add 1 to probability board for ship positions
						for(int k = j; k <= j + shipLength - 1; k++) {
							probabilities[i][k]++;
						}
					}
				}
			}
			//check for vertical ships
			for(int i = 0; i < board[0].length; i++) {
				for(int j = 0; j < board.length - shipLength + 1; j++) {
					//check if [i,j] to [i,j+shipLength-1] inclusive is free for a ship
					boolean free = true;
					for(int k = j; k <= j + shipLength - 1; k++) {
						if(board[k][i] == 'o') {
							free = false;
						}
					}
					if(free) {
						//add 1 to probability board for ship positions
						for(int k = j; k <= j + shipLength - 1; k++) {
							probabilities[k][i]++;
						}
					}
				}
			}
		}

		//give bonus for adjacent squares of known ship 
		int adjacencyBonus = 100;
		for(int y = 0; y < board.length; y++) {
			for(int x = 0; x < board[0].length; x++) {
				if(board[y][x] == 'o') {
					probabilities[y][x] = 0;
				}
				if(board[y][x] == 'x') {
					//this cannot be a possible square
					probabilities[y][x] = 0;

					//check adjacents
					if(y != 0) {
						//check above
						if(board[y-1][x] == '.') {
							probabilities[y-1][x] += adjacencyBonus;
						}
					}
					if(y != board.length -1) {
						//check below
						if(board[y+1][x] == '.') {
							probabilities[y+1][x] += adjacencyBonus;
						}
					}
					if(x != 0) {
						//check left
						if(board[y][x-1] == '.') {
							probabilities[y][x-1] += adjacencyBonus;
						}
					}
					if(x != board[0].length - 1) {
						//check right
						if(board[y][x+1] == '.') {
							probabilities[y][x+1] += adjacencyBonus;
						}
					}
				}
			}
		}
		
		//choose a square with the highest probability
		List<int[]> bestMoves = new ArrayList<int[]>();
		int maxProb = -1;
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[0].length; j++) {
				maxProb = Math.max(maxProb, probabilities[i][j]);
			}
		}
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[0].length; j++) {
				if(probabilities[i][j] == maxProb) {
					bestMoves.add(new int[] {i, j});
				}
			}
		}

		if(showProbabilityBoard) {
			out("probability board: ");
			printBoard(probabilities);
		}

		int[] bestMove = new int[2];
		do {
			bestMove = bestMoves.get(randInt(0,bestMoves.size()-1));
		} while (board[bestMove[0]][bestMove[1]] == 'o' || board[bestMove[0]][bestMove[1]] == 'x');
		return bestMove;
	}

	//exclusive random integer
	static int randInt(int min, int max) {
		return min + (int)(Math.random() * ((max - min) + 1));
	}

	public static void fill(char[][] arr, char val) {
		for(int i = 0; i < arr.length; i++) {
			for(int j = 0; j < arr[0].length; j++) {
				arr[i][j] = val;
			}
		}
	}

	public static boolean isAllZero(int[] arr) {
		for(int i : arr) {
			if(i != 0) {
				return false;
			}
		}
		return true;
	}

	public static void out(Object msg) {
		System.out.println(msg);
	}

	public static void printBoard(char[][] board) {
		String output = "    ";
		for(int i = 0; i < board[0].length; i++) {
			if(alphanumericCoords) {
				output += String.valueOf((char)('A'+i));
				output += " ";
			} else {
				output += (i+1);
				output += " ";
			}
		}
		output += "\n";
		for(int i = 0; i < board.length; i++) {
			if(String.valueOf(i+1).length() == 2) {
				output += (i+1);
				output += ": ";
			} else {
				output += " ";
				output += (i+1);
				output += ": ";
			}
			for(int j = 0; j < board[i].length; j++) {
				output += board[i][j];
				output += " ";
			}
			output += "\n";
		}
		out(output);
	}

	public static void printBoard(int[][] board) {
		String output = "";
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j++) {
				output += board[i][j];
				output += " ";
			}
			output += "\n";
		}
		out(output);
	}
}