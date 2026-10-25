package com.yunying.music;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** 个人云盘设备会话签名，使用平台无 secp256k1 提供器时也可运行的有限域运算。 */
final class AliyunDevice {
    private static final BigInteger P=new BigInteger("fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f",16);
    private static final BigInteger N=new BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141",16);
    private static final Point G=new Point(new BigInteger("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798",16),new BigInteger("483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8",16));
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final String APP_ID="5dde4e1bdf9e4966b387ba58f4b3fdc3";
    static final class Point {final BigInteger x,y;Point(BigInteger x,BigInteger y){this.x=x;this.y=y;}}
    static String privateKey(){BigInteger key;do{key=new BigInteger(256,RANDOM);}while(key.signum()==0||key.compareTo(N)>=0);return hex(key);}
    private static BigInteger key(String hex){BigInteger key=new BigInteger(hex,16);if(key.signum()<=0||key.compareTo(N)>=0)throw new IllegalArgumentException("设备密钥无效");return key;}
    private static Point add(Point a,Point b){
        if(a==null)return b;if(b==null)return a;
        if(a.x.equals(b.x)&&!a.y.equals(b.y))return null;
        BigInteger slope;
        if(a.x.equals(b.x)){if(a.y.signum()==0)return null;slope=a.x.multiply(a.x).multiply(BigInteger.valueOf(3)).multiply(a.y.shiftLeft(1).modInverse(P)).mod(P);}
        else slope=b.y.subtract(a.y).multiply(b.x.subtract(a.x).mod(P).modInverse(P)).mod(P);
        BigInteger x=slope.multiply(slope).subtract(a.x).subtract(b.x).mod(P);
        return new Point(x,slope.multiply(a.x.subtract(x)).subtract(a.y).mod(P));
    }
    private static Point multiply(Point point,BigInteger scalar){Point result=null;for(int i=0;i<scalar.bitLength();i++){if(scalar.testBit(i))result=add(result,point);point=add(point,point);}return result;}
    private static String hex(BigInteger value){String s=value.toString(16);StringBuilder out=new StringBuilder();for(int i=s.length();i<64;i++)out.append('0');return out.append(s).toString();}
    static String publicKey(String secret){Point point=multiply(G,key(secret));return "04"+hex(point.x)+hex(point.y);}
    static String signature(String secret,String device,String user)throws Exception {
        byte[] hash=MessageDigest.getInstance("SHA-256").digest((APP_ID+":"+device+":"+user+":0").getBytes("UTF-8"));
        BigInteger z=new BigInteger(1,hash),d=key(secret);
        while(true){BigInteger k=key(privateKey());Point rPoint=multiply(G,k);BigInteger r=rPoint.x.mod(N);if(r.signum()==0)continue;
            BigInteger s=k.modInverse(N).multiply(z.add(r.multiply(d))).mod(N);if(s.signum()==0)continue;
            int recovery=(rPoint.x.compareTo(N)>=0?2:0)|(rPoint.y.testBit(0)?1:0);
            if(s.compareTo(N.shiftRight(1))>0){s=N.subtract(s);recovery^=1;}
            return hex(r)+hex(s)+"0"+recovery;
        }
    }
    static boolean verify(String secret,String device,String user,String encoded)throws Exception {
        if(encoded.length()!=130)return false;
        BigInteger r=new BigInteger(encoded.substring(0,64),16),s=new BigInteger(encoded.substring(64,128),16);
        if(r.signum()<=0||r.compareTo(N)>=0||s.signum()<=0||s.compareTo(N)>=0)return false;
        BigInteger z=new BigInteger(1,MessageDigest.getInstance("SHA-256").digest((APP_ID+":"+device+":"+user+":0").getBytes("UTF-8")));
        BigInteger w=s.modInverse(N);Point result=add(multiply(G,z.multiply(w).mod(N)),multiply(multiply(G,key(secret)),r.multiply(w).mod(N)));
        return result!=null&&result.x.mod(N).equals(r);
    }
}
