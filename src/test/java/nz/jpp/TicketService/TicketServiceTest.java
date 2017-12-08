package nz.jpp.TicketService;

import org.junit.Test;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;


public class TicketServiceTest 
{
	
	private Set<Integer> intSetGenerator(Integer...integers) {
		HashSet<Integer> set = new HashSet<Integer>();
		for (Integer item : integers) set.add(item);
		return set;
	}

    /**
     * Test basic finding and reserving functions, make sure expired tickets are reserved
     */
	@Test
    public void testBasicFindAndReserve()
    {
        SeatAllocator allocator = mock(SeatAllocator.class);
        when(allocator.getSeats(1)).thenReturn(intSetGenerator(1));
        when(allocator.getSeats(2)).thenReturn(intSetGenerator(2, 3));
        when(allocator.getSeats(3)).thenReturn(intSetGenerator(4, 5, 6));

        TicketService ts = new TicketServiceImpl(allocator, 2);
        
        ts.findAndHoldSeats(1, "zoidberg@freemail.web");        
        SeatHold benderSeats = ts.findAndHoldSeats(2, "bender@ilovebender.com");        
        SeatHold amySeats = ts.findAndHoldSeats(3, "awong79@marslink.web");        
        
        verify(allocator, times(3)).getSeats(anyInt());
        
        //check that invalid reserve requests return errors
        assertEquals(TicketService.SEAT_HOLD_ID_NOT_FOUND, ts.reserveSeats(benderSeats.getSeatHoldId(), "lkajsdfklsdj@lakjsdf.com"));
        assertEquals(TicketService.SEAT_HOLD_ID_NOT_FOUND, ts.reserveSeats(100, benderSeats.getEmail()));
        //check that a valid reserve request succeeds. 
        String benderReservation = ts.reserveSeats(benderSeats.getSeatHoldId(), benderSeats.getEmail());
        try {
        		Integer.parseInt(benderReservation,16);       
        } catch (NumberFormatException e ) {
        		fail("reservation was not hex. Was it an error? reservation: " + benderReservation);
        }
      
        //check that the timed out requests return seats to the allocator.
        verify(allocator, timeout(4000)).returnSeat(1);
        verify(allocator, timeout(4000)).returnSeat(4);
        verify(allocator, timeout(4000)).returnSeat(5);
        verify(allocator, timeout(4000)).returnSeat(6);
        
        //check that timed out seats can no longer be reserved under the same id
        assertEquals(TicketService.SEAT_HOLD_ID_NOT_FOUND, ts.reserveSeats(amySeats.getSeatHoldId(), amySeats.getEmail()));
        
        System.out.println("testBasicFindAndReserve done!");
    }
    
	@Test
    public void testInOrderAllocation() {
    	
    		InOrderSeatAllocator allocator = new InOrderSeatAllocator(3,5); 
    		InOrderSeatAllocator spyAllocator = spy(allocator);
    	
    		TicketService ts = new TicketServiceImpl(spyAllocator, 2);
    	
        SeatHold zoidbergSeats = ts.findAndHoldSeats(1, "zoidberg@freemail.web");        
        SeatHold benderSeats = ts.findAndHoldSeats(5, "bender@ilovebender.com");        
        SeatHold amySeats = ts.findAndHoldSeats(3, "awong79@marslink.web");   

        assertTrue(intSetGenerator(101).equals(zoidbergSeats.getSeats()));
        //bender tried to allocate more than were available in the row, jumps to the next
        assertTrue(intSetGenerator(201, 202, 203, 204, 205).equals(benderSeats.getSeats()));
        //there are still enough in row 1 for amy
        assertTrue(intSetGenerator(102, 103, 104).equals(amySeats.getSeats()));
        assertEquals(null, ts.findAndHoldSeats(6, "bender@ilovebender.com"));
        
        assertEquals(null, ts.findAndHoldSeats(0, "bender@ilovebender.com"));
        
        String benderReservation = ts.reserveSeats(benderSeats.getSeatHoldId(), benderSeats.getEmail());
        try {
        		Integer.parseInt(benderReservation,16);       
        } catch (NumberFormatException e ) {
        		fail("reservation was not hex. Was it an error? reservation: " + benderReservation);
        }
      
        //check that the timed out requests return seats to the allocator.
        verify(spyAllocator, timeout(4000)).returnSeat(101);
        verify(spyAllocator, timeout(4000)).returnSeat(102);
        verify(spyAllocator, timeout(4000)).returnSeat(103);
        verify(spyAllocator, timeout(4000)).returnSeat(104);
        
        SeatHold amyTriesAgain = ts.findAndHoldSeats(3, "awong79@marslink.web");
        assertTrue(intSetGenerator(101, 102, 103).equals(amyTriesAgain.getSeats()));

        System.out.println("testInOrderAllocation done!");
    }
	
	@Test
    public void testFrontAndCenterAllocation() {
    	
		FrontAndCenterSeatAllocator allocator = new FrontAndCenterSeatAllocator(3,5); 
		FrontAndCenterSeatAllocator spyAllocator = spy(allocator);
    	
    		TicketService ts = new TicketServiceImpl(spyAllocator, 2);
    	
        SeatHold zoidbergSeats = ts.findAndHoldSeats(1, "zoidberg@freemail.web");        
        SeatHold benderSeats = ts.findAndHoldSeats(5, "bender@ilovebender.com");        
        SeatHold amySeats = ts.findAndHoldSeats(3, "awong79@marslink.web");   

        assertTrue(intSetGenerator(103).equals(zoidbergSeats.getSeats()));
        //bender tried to allocate more than were available in the row, jumps to the next
        assertTrue(intSetGenerator(201, 202, 203, 204, 205).equals(benderSeats.getSeats()));
        //there are still enough in row 1 for amy
        assertTrue(intSetGenerator(302, 303, 304).equals(amySeats.getSeats()));
        assertEquals(null, ts.findAndHoldSeats(6, "bender@ilovebender.com"));
        
        assertEquals(null, ts.findAndHoldSeats(0, "bender@ilovebender.com"));
        
        String benderReservation = ts.reserveSeats(benderSeats.getSeatHoldId(), benderSeats.getEmail());
        try {
        		Integer.parseInt(benderReservation,16);       
        } catch (NumberFormatException e ) {
        		fail("reservation was not hex. Was it an error? reservation: " + benderReservation);
        }
      
        //check that the timed out requests return seats to the allocator.
        verify(spyAllocator, timeout(4000)).returnSeat(103);
        verify(spyAllocator, timeout(4000)).returnSeat(302);
        verify(spyAllocator, timeout(4000)).returnSeat(303);
        verify(spyAllocator, timeout(4000)).returnSeat(304);
        
        SeatHold amyTriesAgain = ts.findAndHoldSeats(3, "awong79@marslink.web");
        assertTrue(intSetGenerator(102, 103, 104).equals(amyTriesAgain.getSeats()));
        
        System.out.println("testFrontAndCenterAllocation done!");
    }
	
	
	@Test
    public void frontAndCenterAllocationRandomized() {
		System.out.println("frontAndCenterAllocationRandomized");

		FrontAndCenterSeatAllocator allocator = new FrontAndCenterSeatAllocator(10,24); 
		FrontAndCenterSeatAllocator spyAllocator = spy(allocator);
    	
    		TicketService ts = new TicketServiceImpl(spyAllocator, 6);
    		
    		System.out.println("Starting Seats: " + ts.numSeatsAvailable());
    		
    		ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    		ArrayList<ScheduledFuture<?>> futures = new ArrayList<ScheduledFuture<?>>();

    		Random rand = new Random();
    		
    		AtomicInteger requestCount = new AtomicInteger();
    		ConcurrentHashMap<Integer,Integer> reservationHistory = new ConcurrentHashMap<Integer,Integer>();
    		
    		Runnable task = () -> {
    			int value = 1 + rand.nextInt(7);
    			
    			SeatHold seatHold = ts.findAndHoldSeats(value, "WeAllHaveTheSameEmail@whatever.com");
    			
    			try {
    				Thread.sleep(500l + rand.nextInt(2000));
    			} catch (Exception e){}
    			
    			if(rand.nextDouble() < 0.8) {
    				String confirmation = ts.reserveSeats(seatHold.getSeatHoldId(), seatHold.getEmail());
    				if(confirmation == TicketService.SEAT_HOLD_ID_EXPIRED || confirmation == TicketService.SEAT_HOLD_ID_NOT_FOUND)
    					System.err.println("Could not reserve seat. That's not expected...");
    				
    				Integer id = requestCount.incrementAndGet();
    				seatHold.getSeats().forEach(seat -> reservationHistory.put(seat, id));
    				
    				int numSeatsAvailable = ts.numSeatsAvailable();
    				if(id % 6 == 0 && numSeatsAvailable > 24) System.out.println("Remaining Seats: " + ts.numSeatsAvailable());
    				else if (ts.numSeatsAvailable() <= 24) {
    					futures.forEach(future -> future.cancel(false));
    					spyAllocator.noteDoneForMocking();
    				}
    			}
    			
    		};

    		for (int i = 1; i <= 6; i++){
	    		int initialDelay = i/2;
	    		int period = i;
	    		futures.add(executor.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS));
    		}
    		
    		verify(spyAllocator, timeout(80000)).noteDoneForMocking();
    		System.out.println("The order in which each seat was reserved (same number = same reservation). 0 indicates unreserved\n");
    		for( int row = 0; row < 10; row++) {
    			for (int column = 0; column < 24; column++) {
    				int id = reservationHistory.getOrDefault((row + 1) * 100 + column + 1, 0);
    				System.out.format("%3d ", id);
    			}
    			System.out.println();
    		}
    		System.out.println();
    			
    		System.out.println("frontAndCenterAllocationRandomized done!");
	}    	
    
    
}
