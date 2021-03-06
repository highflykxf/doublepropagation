package cike.plan2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

import cike.util.WordNetTools;

public class WeightGetting {

	public static void main(String[] args) throws FileNotFoundException {
		
		Scanner in = new Scanner(new File("C:/Users/Qixuan/Desktop/label/pairs-rel.txt"));
		
		ArrayList<ArrayList<String>> pairsList = new ArrayList<ArrayList<String>>();
		
		for(int asi=0;asi<17;asi++){
			ArrayList<String> pairs = new ArrayList<String>();
			
			in.nextLine();//去除掉首行"Aspect n:"
			while(in.hasNext()){
				
				String line = in.nextLine();
				if(line.equals("------------------"))
					break;
				else{
					String[] lines = line.split(" ");
					String opw = lines[0];
					int freq = Integer.valueOf(lines[1]); 
					//TODO 这个freq的threshold需要调整
					if(freq<2||opw.equals("-lrb-")||opw.equals("-rrb-"))
						continue;
					else{
						pairs.add(opw);
					}
				}
			}
			pairsList.add(pairs);
		}
		
		PrintWriter opwOut = new PrintWriter(new File("C:/Users/Qixuan/Desktop/label/opinionWords.txt"));
		for(ArrayList<String> pairs: pairsList){
			for(String pair: pairs){
				opwOut.print(pair+" ");
			}
			opwOut.println();
		}
		opwOut.close();
		
		
		
		//TODO 暂且只考虑4种
		//同义词weight矩阵
		ArrayList<ArrayList<ArrayList<Boolean>>> synList = new ArrayList<ArrayList<ArrayList<Boolean>>>();
		//反义词weight矩阵
		ArrayList<ArrayList<ArrayList<Boolean>>> atnList = new ArrayList<ArrayList<ArrayList<Boolean>>>();
		//co-appearance关系weight矩阵
		ArrayList<ArrayList<ArrayList<Boolean>>> coList = new ArrayList<ArrayList<ArrayList<Boolean>>>();
		//but关系weight矩阵
		ArrayList<ArrayList<ArrayList<Boolean>>> butList = new ArrayList<ArrayList<ArrayList<Boolean>>>();
		
		
		/**
		 * 记着很重要一点，neg-small这种形式要特殊处理一下（近义词还有反义词那里）
		 * 必须保留是因为：在corpus中会有这种linguistic relation
		 */
		
		Scanner coIn = new Scanner(new File("C:/Users/Qixuan/Desktop/label/co_appearance.txt"));
		
		
		//矩阵列表synList,atnList建立
		//每个列表的每个Aspect单独一个Matrix
		for(int asi=0;asi<17;asi++){
			//第asi个aspect的opinion words数组
			ArrayList<String> pairs = pairsList.get(asi);
			
			ArrayList<ArrayList<Boolean>> synMatrix = new ArrayList<ArrayList<Boolean>>();
			ArrayList<ArrayList<Boolean>> atnMatrix = new ArrayList<ArrayList<Boolean>>();
			ArrayList<ArrayList<Boolean>> coMatrix = new ArrayList<ArrayList<Boolean>>();
			ArrayList<ArrayList<Boolean>> butMatrix = new ArrayList<ArrayList<Boolean>>();
			
			int n = pairs.size();
			
			//初始化同义词矩阵
			for(int i=0;i<n;i++){
				ArrayList<Boolean> synVec = new ArrayList<Boolean>();
				for(int j=0;j<n;j++){
					synVec.add(false);
				}
				synMatrix.add(synVec);
			}
			//初始化反义词矩阵
			for(int i=0;i<n;i++){
				ArrayList<Boolean> atnVec = new ArrayList<Boolean>();
				for(int j=0;j<n;j++){
					atnVec.add(false);
				}
				atnMatrix.add(atnVec);
			}
			//初始化同出现词矩阵
			for(int i=0;i<n;i++){
				ArrayList<Boolean> coVec = new ArrayList<Boolean>();
				for(int j=0;j<n;j++){
					coVec.add(false);
				}
				coMatrix.add(coVec);
			}
			//初始化but隔断词矩阵
			for(int i=0;i<n;i++){
				ArrayList<Boolean> butVec = new ArrayList<Boolean>();
				for(int j=0;j<n;j++){
					butVec.add(false);
				}
				butMatrix.add(butVec);
			}
			
			
			//根据co-appearance操作
			System.out.println(asi);
			coIn.nextLine();
			while(coIn.hasNext()){
				String line = coIn.nextLine();
				if(line.equals("------------------"))
					break;
				else{
					
					String[] halves = line.split(",,");
					String[] half1 = halves[0].split(" ");
					String[] half2 = halves[1].split(" ");
					
					for(String inOne: half2){
						if(pairs.contains(inOne)){
							for(String inTwo: half2){
								if(pairs.contains(inTwo)){
									//如果这个后半部分的一个形容词也在我们的矩阵中
									int idxOne = pairs.indexOf(inOne);
									int idxTwo = pairs.indexOf(inTwo);
									coMatrix.get(idxOne).set(idxTwo, true);
									coMatrix.get(idxTwo).set(idxOne, true);
								}
							}
						}
					}//前半部分

					for(String inOne: half1){
						if(pairs.contains(inOne)){
							//如果这个前半部分的一个形容词在我们的矩阵中
							for(String inTwo: half2){
								if(pairs.contains(inTwo)){
									//如果这个后半部分的一个形容词也在我们的矩阵中
									int idxOne = pairs.indexOf(inOne);
									int idxTwo = pairs.indexOf(inTwo);
									if(idxOne!=idxTwo){
										butMatrix.get(idxOne).set(idxTwo, true);
										butMatrix.get(idxTwo).set(idxOne, true);
									}
								}
							}

							for(String alsoInOne: half1){
								if(pairs.contains(alsoInOne)){
									//如果这个后半部分的一个形容词也在我们的矩阵中
									int idxOne = pairs.indexOf(inOne);
									int idxTwo = pairs.indexOf(alsoInOne);
									coMatrix.get(idxOne).set(idxTwo, true);
									coMatrix.get(idxTwo).set(idxOne, true);
								}
							}
						}
					}//后半部分
					
				}
			}
			
			
			
			//根据同义词、反义词设置矩阵
			for(int i=0;i<n;i++){
				String query = pairs.get(i);
				if(query.contains("neg-")){
					query = query.substring(4);
					LinkedList<String> atn = WordNetTools.getAntonyms2(query);
					LinkedList<String> syn = WordNetTools.getSynonyms(query);
					if(!syn.contains(query))
						syn.add(query);
					
					for(int j=0;j<n;j++){
						String yword = pairs.get(j);
						if(yword.contains("neg-")){
							yword = yword.substring(4);
							if(syn.contains(yword)){
								synMatrix.get(i).set(j, true);
								synMatrix.get(j).set(i, true);
							}
							else if(atn.contains(yword)){
								atnMatrix.get(i).set(j, true);
								atnMatrix.get(j).set(i, true);
							}
						}
						else{
							if(atn.contains(yword)){
								synMatrix.get(i).set(j, true);
								synMatrix.get(j).set(i, true);
							}
							if(syn.contains(yword)){
								atnMatrix.get(i).set(j, true);
								atnMatrix.get(j).set(i, true);
							}
						}
						if(i==j){
							synMatrix.get(i).set(j, true);
							atnMatrix.get(i).set(j, false);
							continue;
						}
					}
				}
				else{
					LinkedList<String> syn = WordNetTools.getSynonyms(query);
					LinkedList<String> atn = WordNetTools.getAntonyms2(query);
					if(!atn.contains("neg-"+query))
						atn.add("neg-"+query);
					
					for(int j=0;j<n;j++){
						String yword = pairs.get(j);
						if(yword.contains("neg-")){
							yword = yword.substring(4);
							if(atn.contains(yword)){
								synMatrix.get(i).set(j, true);
								synMatrix.get(j).set(i, true);
							}
							if(syn.contains(yword)){
								atnMatrix.get(i).set(j, true);
								atnMatrix.get(j).set(i, true);
							}
						}
						else{
							if(syn.contains(yword)){
								synMatrix.get(i).set(j, true);
								synMatrix.get(j).set(i, true);
							}
							else if(atn.contains(yword)){
								atnMatrix.get(i).set(j, true);
								atnMatrix.get(j).set(i, true);
							}
						}
						if(i==j){
							synMatrix.get(i).set(j, true);
							atnMatrix.get(i).set(j, false);
							continue;
						}
					}
				}/***************************/
			}
			
			synList.add(synMatrix);
			atnList.add(atnMatrix);
			coList.add(coMatrix);
			butList.add(butMatrix);
		}
		
		
		PrintWriter synout = new PrintWriter(new File("C:/Users/Qixuan/Desktop/label/SynWeight.txt"));
		PrintWriter atnout = new PrintWriter(new File("C:/Users/Qixuan/Desktop/label/AtnWeight.txt"));
		PrintWriter coout = new PrintWriter(new File("C:/Users/Qixuan/Desktop/label/CoWeight.txt"));
		PrintWriter butout = new PrintWriter(new File("C:/Users/Qixuan/Desktop/label/ButWeight.txt"));
		
		for(int i=0;i<17;i++){
			synout.println("Aspect: "+(i+1));
			atnout.println("Aspect: "+(i+1));
			coout.println("Aspect: "+(i+1));
			butout.println("Aspect: "+(i+1));
			int wi = 1;
			for(String word: pairsList.get(i)){
				synout.print(word+'-'+wi+'\t');
				atnout.print(word+'-'+wi+'\t');
				coout.print(word+'-'+wi+'\t');
				butout.print(word+'-'+wi+'\t');
				
				wi++;
			}
			
			synout.println();
			atnout.println();
			coout.println();
			butout.println();
			
			ArrayList<ArrayList<Boolean>> synMatrix = synList.get(i);
			//Warshall计算出同义词的传递闭包
			Warshall.run(synMatrix);
			
			for(ArrayList<Boolean> synVector: synMatrix){
				for(boolean synEle: synVector){
					synout.print((synEle?1:0)+" ");
				}
				synout.println();
			}
			
			ArrayList<ArrayList<Boolean>> atnMatrix = atnList.get(i);
			//Warshall计算出反义词的传递闭包
			Warshall.run(atnMatrix);
			
			for(ArrayList<Boolean> atnVector: atnMatrix){
				for(boolean atnEle: atnVector){
					atnout.print((atnEle?1:0)+" ");
				}
				atnout.println();
			}
			
			ArrayList<ArrayList<Boolean>> coMatrix = coList.get(i);
/*//			Warshall计算出共同出现的传递闭包
			Warshall.run(coMatrix);*/
			
			for(ArrayList<Boolean> coVector: coMatrix){
				for(boolean coEle: coVector){
					coout.print((coEle?1:0)+" ");
				}
				coout.println();
			}
			
			ArrayList<ArrayList<Boolean>> butMatrix = butList.get(i);
			//Warshall计算出共同出现的传递闭包
			Warshall.run(butMatrix);
			
			for(ArrayList<Boolean> butVector: butMatrix){
				for(boolean butEle: butVector){
					butout.print((butEle?1:0)+" ");
				}
				butout.println();
			}
		}
		
		synout.close();
		atnout.close();
		coout.close();
		butout.close();
	}
}
