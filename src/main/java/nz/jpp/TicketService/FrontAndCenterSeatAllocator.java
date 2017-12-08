package nz.jpp.TicketService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

class Seat {
	private Seat leftNeighbor = null;
	private Seat rightNeighbor = null;
	private Boolean available = true;
	private final double score;
	private final int seatNum; 
	
	public Seat(int seatNum, double score, Seat leftNeighbor) {
		this.seatNum = seatNum;
		this.score = score;
		this.leftNeighbor = leftNeighbor;
		if(leftNeighbor != null)
			leftNeighbor.setRightNeighbor(this);
	}
	
	private void setRightNeighbor(Seat rightNeighbor) {
		this.rightNeighbor = rightNeighbor;
	}
	
	public Boolean isAvailable() {
		return available;
	}
	
	public void setAvailable(Boolean available) {
		this.available = available;
	}
	
	public double getScore() {
		return score;
	}
	
	public int getSeatNum() {
		return seatNum;
	}
	
	public Seat getLeftNeighbor() {
		return leftNeighbor;
	}

	public Seat getRightNeighbor() {
		return rightNeighbor;
	}
}

class SeatComparator implements Comparator<Seat>
{
    @Override
    public int compare(Seat x, Seat y)
    {
    		if(x == null || y == null) throw new NullPointerException("Null seat");
    		
    		//we want higher scored seats first so return "less" when x is greater than y
    		if(x.getScore() > y.getScore()) return -1;
    		else if (x.getScore() == y.getScore()) return 0;
    		else return 1;
    }
}



/*
 * The goal of this Allocator is to try and get people front and center. 
 * As the center front fills, it will try and make the tradeoff between being
 * in the the center vs close to the front.
 */
public class FrontAndCenterSeatAllocator implements SeatAllocator {
	
	HashMap<Integer,Seat> unavailableSeats = new HashMap<Integer,Seat>();
	ArrayList<PriorityQueue<Seat>> rowQueues = new ArrayList<PriorityQueue<Seat>>();
	
	private double columnToRank(int column, int columns) {
		return - Math.pow(column / (columns/2.0 + 0.5) - 1, 2) + 0.8; 
	}
	public FrontAndCenterSeatAllocator(int rows, int columns) {
		Seat leftNeighbor = null;
		for(int row = 1; row <= rows; row++) {
			PriorityQueue<Seat> rowQueue = new PriorityQueue<Seat>(columns, new SeatComparator());
			for(int column = 1; column <= columns; column++) {
				double rank = (rows - row) * 0.2 + columnToRank(column, columns);
				Seat newSeat = new Seat(row * 100 + column, rank, leftNeighbor);
//				System.out.println("adding seat " + newSeat.getSeatNum() + " with score " + newSeat.getScore());
				rowQueue.add(newSeat);
				leftNeighbor = newSeat;
			}
			rowQueues.add(rowQueue);
			leftNeighbor = null;
		}
	}

	
	@Override
	public synchronized int numSeatsAvailable() {
		return rowQueues.stream().mapToInt(rowQueue -> rowQueue.size()).sum();
	}
	
	private HashSet<Seat> getBestAvailableNeighbor(Seat left, Seat right, HashSet<Seat> visited, int wantSize) {
		if(visited.size() == wantSize) return visited;

		Seat testLeft = null;
		if(left != null && left.getLeftNeighbor() != null && left.getLeftNeighbor().isAvailable()) 
			testLeft = left.getLeftNeighbor();

		Seat testRight = null;
		if(right != null && right.getRightNeighbor() != null && right.getRightNeighbor().isAvailable()) 
			testRight = right.getRightNeighbor();
		
		if(testLeft == null && testRight == null) return visited;
		else if(testLeft != null && testRight == null) {
			visited.add(testLeft);
			return getBestAvailableNeighbor(testLeft, right, visited, wantSize);
		}
		else if(testLeft == null && testRight != null) {
			visited.add(testRight);
			return getBestAvailableNeighbor(left, testRight, visited, wantSize);
		}
		else if (testLeft.getScore() >= testRight.getScore()){
			visited.add(testLeft);
			return getBestAvailableNeighbor(testLeft, right, visited, wantSize);			
		} else {
			visited.add(testRight);
			return getBestAvailableNeighbor(left, testRight, visited, wantSize);			
		}
	}

	
	private void dumpPriorityQueue(PriorityQueue<Seat> queue, String prefix) {
		StringBuilder sb = new StringBuilder(prefix);
		queue.stream().forEach(seat -> sb.append(" " + seat.getSeatNum()));
		System.out.println(sb);
	}
	
	private void dumpHashSet(HashSet<Seat> queue, String prefix) {
		StringBuilder sb = new StringBuilder(prefix);
		queue.stream().forEach(seat -> sb.append(" " + seat.getSeatNum()));
		System.out.println(sb);
	}
	
	private void dumpRowQueues() {
		for (int row = 0; row < rowQueues.size(); row++) {
			dumpPriorityQueue(rowQueues.get(row), "Row " + row + " contains seats: ");
		}
	}

	@Override
	public synchronized Set<Integer> getSeats(int numSeats) {
		if(numSeats == 0) return new HashSet<Integer>();
		double bestScore = 0;
		int bestSeatRowNum = -1;
		HashSet<Seat> bestSeats = new HashSet<Seat>();
				
//		dumpRowQueues();
		
		for (int rowNum = 0; rowNum < rowQueues.size(); rowNum++) {
			PriorityQueue<Seat> row = new PriorityQueue<Seat>(rowQueues.get(rowNum));
			if(row.isEmpty()) continue;


			HashSet<Seat> rowBestSeats = new HashSet<Seat>();
			do  {
				rowBestSeats.clear();
//				dumpPriorityQueue(row, "row " + rowNum + " seats starting: ");
//				dumpHashSet(rowBestSeats, "best seats length: " + rowBestSeats.size() + " start: ");
				
				
				Seat start = row.remove();
				rowBestSeats.add(start);
				
				Seat left = start.getLeftNeighbor();
				if(left != null && !left.isAvailable()) left = null;
				Seat right = start.getRightNeighbor();
				if(right != null && !right.isAvailable()) right = null;
				
				if(numSeats == 1 || (left == null && right == null)) {}
				else if(left != null && right == null) {
					rowBestSeats.add(left);
					getBestAvailableNeighbor(left, start, rowBestSeats, numSeats);
				}
				else if(left == null && right != null) {
					rowBestSeats.add(right);
					getBestAvailableNeighbor(start, right, rowBestSeats, numSeats);
				}
				else if (left.getScore() >= right.getScore()){
					rowBestSeats.add(left);
					getBestAvailableNeighbor(left, start, rowBestSeats, numSeats);			
				} else {
					rowBestSeats.add(right);
					getBestAvailableNeighbor(start, right, rowBestSeats, numSeats);			
				}
				
				//always remove rowBestSeats from the row so we don't try them again
				row.removeAll(rowBestSeats);
				//dumpPriorityQueue(row, "row " + rowNum + " seats remaining: ");
				//dumpHashSet(rowBestSeats, "best seats found: ");
			} while(rowBestSeats.size() != numSeats && !row.isEmpty());
			
			if(rowBestSeats.size() == numSeats) {
				//yay! we found the best set in this row
				double rowScore = rowBestSeats.stream().mapToDouble(seat -> seat.getScore()).sum();
				if(rowScore > bestScore) {
					//Yay! these are the best seats so far
					bestSeats.clear();
					bestSeats.addAll(rowBestSeats);
					bestScore = rowScore;
					bestSeatRowNum = rowNum;
				}
				//we could probably 	optimize this by not searching further rows when they can't
				//possibly beat the current best found seats. Maybe do that later...

			}
			
		}
		
		HashMap<Integer,Seat> seatsToReturn = new HashMap<Integer,Seat>();
		bestSeats.forEach(seat -> {
			seatsToReturn.put(seat.getSeatNum(), seat);
			seat.setAvailable(false);
		});
		
		unavailableSeats.putAll(seatsToReturn);
		if(bestSeatRowNum >= 0) {
			rowQueues.get(bestSeatRowNum).removeAll(bestSeats);
		}
		
//		dumpRowQueues();
		
		return new HashSet<Integer>(seatsToReturn.keySet());
	}

	@Override
	public synchronized void returnSeat(Integer seatNum) {
		Seat seat = unavailableSeats.get(seatNum);
		seat.setAvailable(true);
		rowQueues.get(seatNum / 100 - 1).add(seat);		
	}
	
	public void noteDoneForMocking() {}

}
