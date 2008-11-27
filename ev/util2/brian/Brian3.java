package util2.brian;


import java.io.*;
import java.sql.*;
import java.util.*;

import endrov.util.EvParallel;

import bioserv.seqserv.io.*;

/**
 * Extract ranking or null
 */
public class Brian3
	{
	
	public static void main(String[] args)
		{
		System.out.println(BrianSQL.connectPostgres("//193.11.32.108/brian", "postgres", "wuermli"));
	
		try
			{
			for(final String otherOrg:new String[]{"ppatens","creinhardtii"})
				{
				//Which records need be done?
				ResultSet rs=
				BrianSQL.runQuery(
						"select cegene from (" +
						"select cegene,organism from reverseblast where organism='"+otherOrg+"' " +
						"and blastout is not null and (cegene,organism) not in (select cegene,organism from blastrank)" +
				") as foo1");
				LinkedList<String> geneTODO=new LinkedList<String>();
				while(rs.next())
					geneTODO.add(rs.getString(1));
				System.out.println("TODO: "+geneTODO.size());
	
				//Do records (in parallel)
				EvParallel.map_(geneTODO, 
						new EvParallel.FuncAB<String, Object>(){
						public Object func(String wbGene)
							{
							try
								{
								//Read BLAST output
								System.out.println(wbGene);
								Blast2 b=Blast2.readModeXML(new StringReader(BrianSQL.getBlastResult("reverseblast", otherOrg, wbGene)));
	
								//What is the rank?
								int rank=0;
								BrianSQL.runUpdate("delete from blastrank where organism='"+otherOrg+"' and cegene='"+wbGene+"'");
								Integer foundRank=null;
								for(Blast2.Entry e:b.entry)
									{
									//TODO: how to parse?
									StringTokenizer stok=new StringTokenizer(e.subjectid,"|");  //WBGene00007201|exos-4.1
									String thisWbGene=stok.nextToken();
									if(thisWbGene.equals(wbGene))
										{
										System.out.println("Found "+wbGene+" rank# "+rank);
										foundRank=rank;
										break;
										}
									rank++;
									}
								
								//TODO: Find E-values, upload these too.
								
								BrianSQL.runUpdate("insert into blastrank values('"+otherOrg+"','"+wbGene+"',"+foundRank+")");
								}
							catch (Exception e)
								{
								e.printStackTrace();
								}
	
							return null;
							}
				});
				}
			}
		catch (SQLException e)
			{
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
		System.out.println("main done");
		}
	
	}