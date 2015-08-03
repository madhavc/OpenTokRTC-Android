package com.tokbox.android.opentokrtc;

/**
 * Created by madhav on 7/15/15.
 */
public class QosFormula {

    public static double calcuateAudioQosPoint(double period_audio_packets, double period_audio_packets_lost_perc)
    {
        double as = -1;

        //period packets has to be more than 0 packets
        if (period_audio_packets > 0 && period_audio_packets_lost_perc >= 0) {
            as = QosFormula.audioScore(period_audio_packets_lost_perc);
        }

        return as;
    }

    public static double calcuateVideoQosPoint(double avg_video_bitrate, int frame_rate, int video_height, int video_width){

        double vs = -1;

        //int avg_video_bitrate = period_video_byterate * 8;

        if (avg_video_bitrate > 0 && frame_rate > 0 && video_height > 0 && video_width > 0) {
            vs = QosFormula.videoScore(avg_video_bitrate, video_height, video_width, frame_rate);
        } else if (avg_video_bitrate > 0) {
            vs = QosFormula.videoScore(avg_video_bitrate);
        }

        return vs;
    }


    public static double videoScore(double avg_video_bitrate, int video_height, int video_width, int frame_rate) {
        // http://community.ooyala.com/t5/Developers-Knowledge-Base/Understanding-Bitrate-Resolution-and-Quality/ta-p/1740
        //        pixels per second = Width x Height x Frames per second ,Bits per pixel (bpp) = video bitrate / pixels per second
        //        Any bpp values around 0.1 have very good quality (higher bitrates won't produce visually significant improvement).
        //        Any bpp values around 0.03 have poor quality (lower bitrates are usually unwatchable).
        double pps = video_height * video_width * frame_rate;
        double bbp = avg_video_bitrate / pps;
        //return bbp;
        return (bbp >= 0.03) ? 1 : 0;
    }

    public static int videoScore(double avg_video_bitrate) {

        double mos = 5 * (1 - Math.exp(-1 * 0.5 * avg_video_bitrate / (1000 * 100)));
        return (mos > 2) ? 1 : 0;
    }

    public static int audioScore(double period_audio_packets_lost_perc) {
//        System.out.println(MOS(R(50, period_audio_packets_lost_perc)));
        return (MOS(R(50, period_audio_packets_lost_perc)) >= 3) ? 1 : 0;
    }


//    //http://www.nas.ewi.tudelft.nl/publications/2006/XZhou_VoIP_MOME2006.pdf
//    //Taken from iLBC but modified assuming better default quality a = 0
    //fixed delay 50,period_packet_loss

    private static final double a = 0; //iLBC a = 10;
    private static final double b = 19.8;
    private static final double c = 29.7;

    //Delay due to packetization, capture buffering and computation
    private static final long LOCAL_DELAY = 30; //30 msecs: typical frame duration

    public static double calc(long getDelay, double getPacketLoss) {
        return MOS(R(getDelay, getPacketLoss));
    }

    //R = 94.2 − Id − Ie
    private static double R(long getDelay, double getPacketLoss) {
        long d = getDelay + LOCAL_DELAY;
        double Id = 0.024 * d + 0.11 * (d - 177.3) * H(d - 177.3);

        double P = getPacketLoss;
        double Ie = a + b * Math.log(1 + c * P);

        double R = 94.2 - Id - Ie;

        return R;
    }

    private static double H(double x) {
        return x < 0 ? 0 : 1;
    }

    //For R < 0: MOS = 1
    //For 0 R 100: MOS = 1 + 0.035 R + 7.10E-6 R(R-60)(100-R)
    //For R > 100: MOS = 4.5
    private static double MOS(double R) {
        if (R < 0) {
            return 1;
        }
        if (R > 100) {
            return 4.5;
        }
        return 1 + 0.035 * R + 7.10 / 1000000 * R * (R - 60) * (100 - R);
    }

}
