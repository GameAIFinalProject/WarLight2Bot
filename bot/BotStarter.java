/**
 * Warlight AI Game Bot
 *
 * Last update: January 29, 2015
 *
 * @author Jim van Eeden
 * @version 1.1
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */

package bot;

/**
 * This is a simple bot that does random (but correct) moves.
 * This class implements the Bot interface and overrides its Move methods.
 * You can implement these methods yourself very easily now,
 * since you can retrieve all information about the match from variable states
 * When the bot decided on the move to make, it returns an ArrayList of Moves. 
 * The bot is started by creating a Parser to which you add
 * a new instance of your bot, and then the parser is started.
 */

import java.awt.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;

public class BotStarter implements Bot 
{
	@Override
	/**
	 * A method that returns which region the bot would like to start on, the pickable regions are stored in the BotState.
	 * The bots are asked in turn (ABBAABBAAB) where they would like to start and return a single region each time they are asked.
	 * This method returns one random region from the given pickable regions.
	 */
	public Region getStartingRegion(BotState state, Long timeOut)
	{
		double rand = Math.random();
		int r = (int) (rand*state.getPickableStartingRegions().size());
		int regionId = state.getPickableStartingRegions().get(r).getId();
		Region startingRegion = state.getFullMap().getRegion(regionId);
		
		return startingRegion;
	}

	@Override
	/**
	 * This method is called for at first part of each round. This example puts two armies on random regions
	 * until he has no more armies left to place.
	 * @return The list of PlaceArmiesMoves for one round
	 */
	public ArrayList<PlaceArmiesMove> getPlaceArmiesMoves(BotState state, Long timeOut) 
	{		
		ArrayList<PlaceArmiesMove> placeArmiesMoves = new ArrayList<PlaceArmiesMove>();
		String myName = state.getMyPlayerName();
		int armies = 2;
		int armiesLeft = state.getStartingArmies();
		LinkedList<Region> visibleRegions = state.getVisibleMap().getRegions();
		
		//reinforce the frontline
		while(armiesLeft > 0)
		{
			//double rand = Math.random();
			//int r = (int) (rand*visibleRegions.size());
			//Region region = visibleRegions.get(r);
			
			for (Region region : visibleRegions)
			{
				if(region.ownedByPlayer(myName))
				{
					if (hasAllFriendlyNeighbors(region, myName) == false)
					{
						placeArmiesMoves.add(new PlaceArmiesMove(myName, region, armies));
						armiesLeft -= armies;
						
						if (armiesLeft <= 0)
						{
							break;
						}
					}
					
				}
			}
		}
		
		return placeArmiesMoves;
	}
	
	private boolean hasAllFriendlyNeighbors(Region fromRegion, String myName)
	{
		ArrayList<Region> possibleToRegions = new ArrayList<Region>();
		possibleToRegions.addAll(fromRegion.getNeighbors());
		
		for (Region toRegion : possibleToRegions)
		{
			if (!toRegion.getPlayerName().equals(myName))
			{
				return false;
			}
		}
		return true;
	}
	
	private ArrayList<Region> getFriendlyNeighbors(Region fromRegion, String myName)
	{
		ArrayList<Region> possibleToRegions = new ArrayList<Region>();
		possibleToRegions.addAll(fromRegion.getNeighbors());
		
		ArrayList<Region> friendlyNeighbors = new ArrayList<Region>();
		for (Region toRegion : possibleToRegions)
		{
			if (toRegion.getPlayerName().equals(myName))
			{
				friendlyNeighbors.add(toRegion);
			}
		}
		
		return friendlyNeighbors;
	}
	
	private ArrayList<Region> getNonFriendlyNeighbors(Region fromRegion, String myName)
	{
		ArrayList<Region> possibleToRegions = new ArrayList<Region>();
		possibleToRegions.addAll(fromRegion.getNeighbors());
		
		ArrayList<Region> nonFriendlyNeighbors = new ArrayList<Region>();
		for (Region toRegion : possibleToRegions)
		{
			if (!toRegion.getPlayerName().equals(myName))
			{
				nonFriendlyNeighbors.add(toRegion);
			}
		}
		
		return nonFriendlyNeighbors;
	}
	
	private HashMap<Region, Integer> getDistanceFromFrontline(LinkedList<Region> allMapRegions, String myName)
	{
		HashMap<Region, Integer> distanceMap = new HashMap<Region, Integer>();
		ArrayList<Region> knownNeighbors = new ArrayList<Region>();
		
		for (Region fromRegion : allMapRegions)
		{
			//skip this region if it isn't ours
			if(!fromRegion.ownedByPlayer(myName)) 
			{
				continue;
			}
			
			ArrayList<Region> possibleToRegions = new ArrayList<Region>();
			possibleToRegions.addAll(fromRegion.getNeighbors());
			
			if (hasAllFriendlyNeighbors(fromRegion, myName) == false)
			{
				distanceMap.put(fromRegion, 1);
				knownNeighbors.add(fromRegion);
			}
		}
		
		//flood fill the entire map
		int numIters = 0;
		while (distanceMap.size() < allMapRegions.size())
		{
			//keep track of all the newly assigned regions
			ArrayList<Region> newKnownRegions = new ArrayList<Region>();
			
			for (Region fromRegion : knownNeighbors)
			{
				//assign a distance value to all friendly neighbors that don't have a value
				ArrayList<Region> friendlyNeighborsList = getFriendlyNeighbors(fromRegion, myName);
				for (Region toRegion : friendlyNeighborsList)
				{
					//don't assign a value if there already exists one
					if (distanceMap.containsKey(toRegion))
					{
						continue;
					}
					
					int currentDistance = distanceMap.get(fromRegion);
					distanceMap.put(toRegion, currentDistance + 1);
					newKnownRegions.add(toRegion);
				}
			}
			
			knownNeighbors = new ArrayList<Region>();
			for (Region reg : newKnownRegions)
			{
				knownNeighbors.add(reg);
			}
			
			numIters++;
			
			//avoid an infinite loop
			if (numIters > 10)
			{
				break;
			}
		}
		
		return distanceMap;
	}
	
	@Override
	/**
	 * This method is called for at the second part of each round. This example attacks if a region has
	 * more than 6 armies on it, and transfers if it has less than 6 and a neighboring owned region.
	 * @return The list of PlaceArmiesMoves for one round
	 */
	public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state, Long timeOut) 
	{
		ArrayList<AttackTransferMove> attackTransferMoves = new ArrayList<AttackTransferMove>();
		String myName = state.getMyPlayerName();
		int armies = 5;
		int maxTransfers = 10;
		int transfers = 0;
		
		HashMap<Region, Integer> distanceMap = getDistanceFromFrontline(state.getVisibleMap().getRegions(), myName);
		
		for(Region fromRegion : state.getVisibleMap().getRegions())
		{
			if(fromRegion.ownedByPlayer(myName)) //do an attack
			{
				ArrayList<Region> possibleToRegions = new ArrayList<Region>();
				possibleToRegions.addAll(fromRegion.getNeighbors());
				
				//bordering non-friendly territory
				if (distanceMap.containsKey(fromRegion) && distanceMap.get(fromRegion) == 1)
				{
					//keep expanding if possible, otherwise stay hold
					
					//Get regions for attacking (attack all regions if the unit ratio >= threshold)
					ArrayList<Region> enemyRegions = getNonFriendlyNeighbors(fromRegion, myName);
					
					int armiesLeft = fromRegion.getArmies();
					for (Region toRegion : enemyRegions)
					{
						int numOpponentUnits = toRegion.getArmies();

						double ratioPlayerOpponentArmy = (double) armiesLeft / numOpponentUnits;

						//if the ratio is greater than a certain threshold, attack
						double threshold = 2.0;
						if (ratioPlayerOpponentArmy >= threshold && armiesLeft >= 5)
						{
							int armiesToSend = armiesLeft - 1;
							armiesLeft -= armiesToSend;	
						
							attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, toRegion, armiesToSend));
							break;
						}
					}
				}
				else
				{
					//transfer units from the back to the front
					//Get regions for transferring 
					ArrayList<Region> neighborRegions = getFriendlyNeighbors(fromRegion, myName);
					for (Region toRegion : neighborRegions)
					{
						//check if closer to frontline
						if (!distanceMap.containsKey(fromRegion) || distanceMap.get(toRegion) < distanceMap.get(fromRegion))
						{
							//if so, transfer all except 1 unit
							int armiesToTransfer = fromRegion.getArmies() - 1;
							
							attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, toRegion, armiesToTransfer));
							break;
						}
					}
				}
				
				/*while(!possibleToRegions.isEmpty())
				{
					double rand = Math.random();
					int r = (int) (rand*possibleToRegions.size());
					Region toRegion = possibleToRegions.get(r);
								
					if(!toRegion.getPlayerName().equals(myName) && fromRegion.getArmies() > 6) //do an attack
					{
						attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, toRegion, armies));
						break;
					}
					else if(toRegion.getPlayerName().equals(myName) && fromRegion.getArmies() > 1
								&& transfers < maxTransfers) //do a transfer
					{
						attackTransferMoves.add(new AttackTransferMove(myName, fromRegion, toRegion, armies));
						transfers++;
						break;
					}
					else
						possibleToRegions.remove(toRegion);
				}*/
			}
		}
		
		return attackTransferMoves;
	}

	public static void main(String[] args)
	{
		BotParser parser = new BotParser(new BotStarter());
		parser.run();
	}

}
