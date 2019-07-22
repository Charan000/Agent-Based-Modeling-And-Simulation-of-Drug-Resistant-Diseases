/*
©Copyright 2012 Nick Malleson
This file is part of RepastCity.

RepastCity is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

RepastCity is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of  the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.
*/

package repastcity3.agent;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;



import repast.simphony.engine.environment.RunEnvironment;
import repastcity3.environment.Building;
import repastcity3.environment.Route;
import repastcity3.main.ContextManager;
import repast.simphony.ui.probe.ProbedProperty;


public class DefaultAgent implements IAgent {


	private static Logger LOGGER = Logger.getLogger(DefaultAgent.class.getName());

	private Building home; // Where the agent lives 
	private Route route; // An object to move the agent around the world
	private Building office;
	private boolean reachedHome = true;
	private boolean reachedOffice = false;// Whether the agent is going to or from their home
	
	private int type;
	private double till_at_home,work_end,liesure_end;
	private DefaultAgentState diseaseState;
	private double radius;

	private static int uniqueID = 0;
	public static int one_tick_sec = 60;
	private static double ticks_day = 24 * 60 * 60 / one_tick_sec;
	
	public static HashMap<DefaultAgentState.State,Integer> typeCount = new  HashMap<DefaultAgentState.State,Integer>() ;
	public static boolean hMapIntialized = false;
	
	
	
	private int id;

	
	

	public DefaultAgent() {
		this.id = uniqueID++;
		
		this.radius = 7 + Math.random();
		
		if(!hMapIntialized)
		{
			for(DefaultAgentState.State s : DefaultAgentState.State.values())
				typeCount.put(s, 0);
			hMapIntialized = true;
		}

		
		
	}

	@Override
	public void step() throws Exception {
		
		double currentTicks = RunEnvironment.getInstance().getCurrentSchedule().getTickCount()%ticks_day;
		double theTime = ContextManager.getRealTime();
		
		
		if(currentTicks == 1)
		{	
		
			if(this.type == 1)
			{
				this.till_at_home = (7 + Math.random());
				this.work_end = (15 + Math.random());
				this.liesure_end = (21 + Math.random());
				
			}
			
			else if(this.type == 2)
			{
				this.till_at_home = (6 + Math.random());
				this.work_end = (17 + Math.random());
				this.liesure_end = (20 + Math.random());
				
			}
			
			else if(this.type == 3)
			{
				double tillHome = 6 + Math.random();
				this.till_at_home = tillHome;
				this.work_end = tillHome + 2;
				this.liesure_end = (21 + Math.random());
				
			}
			
			else if(this.type == 4)
			{
				;
			}

		}
		
		else if(theTime < till_at_home  && theTime != 1) 
		{	
			//System.out.println("AT HOME "+theTime);
			
			
			if(this.reachedHome == false)
			{
				if(this.route == null)
				{	
					Building house = this.home;
					this.route = new Route(this, house.getCoords(), house);
					
				}
				
				if (!this.route.atDestination()) {
					this.route.travel();
					LOGGER.log(Level.FINE, this.toString() + " travelling to " + this.route.getDestinationBuilding().toString());
				} 
				else {
					this.reachedHome = true;
					this.reachedOffice = false;
					this.route = null;
				
				}
			}
		}
		
		else if(theTime > till_at_home && theTime < work_end )
		{
			//System.out.println("AT WORK "+theTime);
		
			if(this.reachedOffice == false) 
			{
				if (this.route == null ) 
				{
					this.reachedHome = false;
					Building b = this.office;
					this.route = new Route(this, b.getCoords(), b);
			
				}
				
				if (!this.route.atDestination()) 
				{
					this.route.travel();
			
				} 
				else 
				{
					this.reachedOffice = true;
					this.reachedHome = false;
					this.route = null;
				}
			}

		}
	
		else if(theTime > work_end  && theTime < liesure_end)
		{
			//System.out.println("AT LIESURE "+theTime);
			if(this.route == null)
			{
				Building randB = ContextManager.buildingContext.getRandomObject();
				this.route = new Route(this, randB.getCoords(), randB);
				
			}
			
			if (!this.route.atDestination()) {
				this.route.travel();
				LOGGER.log(Level.FINE, this.toString() + " travelling to " + this.route.getDestinationBuilding().toString());
			} 
			else {
				
				Building randB = ContextManager.buildingContext.getRandomObject();
				this.route = new Route(this, randB.getCoords(), randB);
			
			
			}
		}
		
		else if(theTime == liesure_end )
		{
			this.route = null;
		}
		
		else if(theTime > liesure_end)
		{	
			//System.out.println("BACK HOME "+theTime);
			if(this.reachedHome == false) 
			{
				if(this.route == null)
				{	
					Building house = this.home;
					this.route = new Route(this, house.getCoords(), house);
				
				}
			
				if(!this.route.atDestination()) 
				{
					this.route.travel();
					LOGGER.log(Level.FINE, this.toString() + " travelling to " + this.route.getDestinationBuilding().toString());
				} 
				else 
				{
				
				this.reachedHome = true;
				this.reachedOffice = false;
				this.route = null;
				}
			}
			
		}
		
	} // step()

	/**
	 * There will be no inter-agent communication so these agents can be executed simulataneously in separate threads.
	 */
	@Override
	public final boolean isThreadable() {
		return true;
	}

	@Override
	public void setHome(Building home) {
		this.home = home;
	}

	@Override
	public Building getHome() {
		return this.home;
	}
	
	public void setType(int t)
	{
		this.type = t;
	}
	
	public double getR()
	{
		return this.radius;
	}
	
	public void setR(double r)
	{
		this.radius = r;
	}
	
	public int getType()
	{
		return this.type;
	}
	
	public int checkU()
	{
		if(this.diseaseState.getCurrentState() == DefaultAgentState.State.U )
		{
			return 1;
		}
		
		return 0;
	}
	
	public int checkAR()
	{
		if(this.diseaseState.getCurrentState() == DefaultAgentState.State.AR )
		{
			return 1;
		}
		
		return 0;
	}
	
	public int checkLS()
	{
		if(this.diseaseState.getCurrentState() == DefaultAgentState.State.LS )
		{
			return 1;
		}
		
		return 0;
	}
	
	public int checkLR()
	{
		if(this.diseaseState.getCurrentState() == DefaultAgentState.State.LR )
		{
			return 1;
		}
		
		return 0;
	}
	
	public int checkAS()
	{
		if(this.diseaseState.getCurrentState() == DefaultAgentState.State.AS )
		{
			return 1;
		}
		
		return 0;
	}
	
	public void setDisease(DefaultAgentState t)
	{
		this.diseaseState = t;
	}
	
	public DefaultAgentState getDisease()
	{
		return this.diseaseState;
	}
	
	public DefaultAgentState.State getCurrentState()
	{
		return this.diseaseState.getCurrentState();
	}
	

	public void setOffice(Building off) {
		this.office = off;
	}

	
	public Building getOffice() {
		return this.office;
	}

	@Override
	public <T> void addToMemory(List<T> objects, Class<T> clazz) {
	}

	@Override
	public List<String> getTransportAvailable() {
		return null;
	}

	@Override
	public String toString() {
		return "Agent " + this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DefaultAgent))
			return false;
		DefaultAgent b = (DefaultAgent) obj;
		return this.id == b.id;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

}
