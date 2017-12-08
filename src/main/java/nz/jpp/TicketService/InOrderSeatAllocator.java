package nz.jpp.TicketService;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InOrderSeatAllocator implements SeatAllocator {
	
	private final TreeSet<Integer> seats; 
	
	
	/*
	 * This is a dumb seat allocator that tries to seat people as close to the front
	 * left as possible (fill out the first row from left-to-right and then row two, etc). 
	*/
	public InOrderSeatAllocator(int rows, int columns) {
		List<Integer> seatList = IntStream.rangeClosed(1, rows).flatMap(row -> 
			IntStream.rangeClosed(1, columns).map(column -> row * 100 + column)
		).boxed().collect(Collectors.toList());
		
		seats = new TreeSet<Integer>(seatList);
	}

	@Override
	public synchronized int numSeatsAvailable() {
		return seats.size();
	}

	@Override
	public synchronized Set<Integer> getSeats(int numSeats) {
		if (numSeats == 0) return new HashSet<Integer>();
				
		Iterator<Integer> seatIter = seats.iterator();
		LinkedList<Integer> seatCandidates = new LinkedList<Integer>();
		if(seatIter.hasNext()) seatCandidates.add(seatIter.next());
		while(seatIter.hasNext() && seatCandidates.size() < numSeats) {
			Integer current = seatIter.next();
			//check that they're adjacent, if they're not clear the candidates
			if(current.intValue() - seatCandidates.peekLast().intValue() != 1)
				seatCandidates.clear();

			seatCandidates.add(current);
		}
		//the candidates are only valid if we have enough to fulfill the request
		if(seatCandidates.size() == numSeats) {
			seats.removeAll(seatCandidates);
		}
		else
			seatCandidates.clear();
		
		return new HashSet<Integer>(seatCandidates);
	}

	@Override
	public synchronized void returnSeat(Integer seat) {
		seats.add(seat);
	}

}
