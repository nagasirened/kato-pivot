package com.kato.pro.sensitive.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 拼音工具类
 * 用于汉字转拼音、拼音匹配等
 */
@Slf4j
@Component
public class PinyinUtil {

    /**
     * 汉字到拼音的首字母映射
     */
    private static final Map<Character, String> PINYIN_MAP = new HashMap<>();

    /**
     * 完整拼音映射（常用汉字）
     */
    private static final Map<Character, String> FULL_PINYIN_MAP = new HashMap<>();

    static {
        // 初始化常用汉字拼音映射
        initPinyinMap();
    }

    private static void initPinyinMap() {
        // 常用汉字拼音映射（部分）
        String[] pinyins = {
                "啊=a", "阿=a", "埃=ai", "挨=ai", "哎=ai", "唉=ai", "哀=ai", "矮=ai",
                "爱=ai", "安=an", "暗=an", "岸=an", "昂=ang", "袄=ao",
                "吧=ba", "八=b", "巴=b", "拔=ba", "把=ba", "坝=ba", "爸=ba", "白=bai", "百=bai", "拜=bai",
                "班=ban", "搬=ban", "板=ban", "半=ban", "办=ban", "扮=ban", "伴=ban", "棒=bang",
                "包=bao", "保=bao", "报=bao", "抱=bao", "贝=bei", "倍=bei", "被=bei", "本=ben",
                "比=bi", "笔=bi", "必=bi", "闭=bi", "边=bian", "变=bian", "便=bian",
                "别=bie", "宾=bin", "兵=bing", "冰=bing", "饼=b", "并=b", "病=bing",
                "播=bo", "拨=bo", "波=bo", "博=bo", "不=bu", "步=bu", "部=bu",
                "猜=cai", "才=cai", "材=cai", "彩=cai", "菜=cai", "操=cao", "草=cao",
                "层=ceng", "查=cha", "茶=cha", "察=cha", "差=cha", "产=chan",
                "常=chang", "场=chang", "唱=chang", "超=chao", "朝=chao", "车=che",
                "陈=chen", "称=cheng", "城=cheng", "成=cheng", "吃=chi", "持=chi",
                "充=chong", "虫=chong", "抽=chou", "出=chu", "除=chu", "处=chu",
                "传=chuan", "船=chuan", "创=chuang", "吹=chui", "春=chun",
                "词=ci", "次=ci", "从=cong", "匆=cong", "村=cun", "存=cun",
                "错=cuo", "打=da", "大=da", "代=dai", "带=dai", "待=dai",
                "单=dan", "但=dan", "蛋=dan", "当=dang", "刀=dao", "倒=dao",
                "到=dao", "道=dao", "得=de", "的=de", "等=deng", "低=di",
                "底=di", "迪=di", "地=di", "弟=di", "帝=di", "点=dian",
                "调=diao", "掉=diao", "顶=ding", "定=ding", "丢=diu", "东=dong",
                "冬=dong", "动=dong", "冻=dong", "都=dou", "读=du", "独=du",
                "度=du", "短=duan", "段=duan", "断=duan", "对=dui", "队=dui",
                "多=duo", "夺=duo", "朵=duo", "我=wo", "们=men", "的=de",
                "是=shi", "不=bu", "在=zai", "有=you", "人=ren", "这=zhe",
                "中=zhong", "大=da", "为=wei", "和=he", "国=guo", "年=nian",
                "来=lai", "上=shang", "到=dao", "说=shuo", "要=yao", "就=jiu",
                "出=chu", "会=hui", "也=ye", "子=zi", "时=shi", "里=li",
                "地=di", "那=na", "得=de", "着=zhuo", "看=kan", "想=xiang",
                "天=tian", "然=ran", "学=xue", "好=hao", "可=ke", "下=xia",
                "过=guo", "能=neng", "对=dui", "自=zi", "己=ji", "身=shen",
                "发=fa", "心=xin", "前=qian", "小=xiao", "她=ta", "些=xie",
                "后=hou", "去=qu", "还=hai", "以=yin", "他=ta", "么=me",
                "同=tong", "之=zhi", "新=xin", "内=nei", "月=yue", "日=ri",
                "水=shui", "无=wu", "手=shou", "信=xin", "表=biao", "示=shi",
                "林=lin", "机=ji", "器=qi", "因=yin", "果=guo", "应=ying",
                "行=xing", "列=lie", "文=wen", "公=gon", "司=si", "各=ge",
                "合=he", "重=zhong", "统=tong", "部=bu", "分=fen", "开=kai",
                "关=guan", "式=shi", "点=dian", "长=chang", "出=chu", "把=ba",
                "机=ji", "用=yong", "面=main", "先=xian", "总=zong", "系=xi",
                "法=f", "化=hua", "建=jie", "立=li", "场=chang", "当=dang",
                "意=yi", "意=yi", "度=du", "门=men", "口=kou", "进=jin", "出=chu",
                "问=wen", "号=hao", "功=gong", "过=guo", "成=cheng", "考=kao",
                "认=ren", "算=suan", "经=jing", "济=ji", "设=s", "别=bie",
                "感=gan", "特=te", "受=shou", "接=jie", "直=zhi", "线=xian",
                "器=qi", "认=ren", "车=che", "间=jian", "输=shu", "完=wan",
                "全=quan", "调=diao", "程=cheng", "期=qi", "难=nan", "改=gai",
                "极=ji", "端=duan", "品=pin", "数=shu", "量=liang", "级=ji",
                "保=bao", "管=guan", "界=jie", "教=jiao", "限=xian", "园=yuan",
                "具=ju", "域=yu", "市=shi", "参=can", "影=ying", "片=pian",
                "管=guan", "议=y", "济=ji", "证=zheng", "决=jue", "缩=suo",
                "交=jiao", "识=shi", "码=ma", "需=xu", "优=you", "势=shi",
                "华=hua", "优=you", "领=ling", "导=dao", "创=chuang", "新=xin",
                "互=hu", "联=lian", "网=wang", "平=t", "台=tai", "数=shu",
                "据=ju", "智=zhi", "能=neng", "云=yun", "计=ji", "移=yi",
                "动=dong", "终=zhong", "端=duan", "安=an", "全=quan", "风=feng",
                "险=xian", "管=guan", "控=kong", "监=jian", "测=ce", "预=yu",
                "警=jing", "响=xiang", "速=su", "度=du", "性=xing", "能=neng",
                "扩=kuo", "容=rong", "弹=tan", "缩=suo", "日=ri", "志=zhi",
                "感=gan", "统=tong", "开=kai", "放=fang", "共=gong", "享=xiang",
                "协=xie", "作=zuo", "模=mo", "式=shi", "架=jia", "构=gou",
                "策=ce", "略=lve", "规=gui", "则=ze", "概=gai", "况=kuang",
                "述=shu", "评=ping", "比=bi", "论=lun", "析=xi", "问=wen",
                "题=ti", "解=jie", "方=fang", "案=an", "论=lun", "点=dian",
                "总=zong", "结=jie", "汇=hui", "编=bian", "译=yi", "审=shen",
                "校=jiao", "对=dui", "错=cuo", "误=wu", "排=bai", "查=cha",
                "找=zhao", "原=yuan", "因=yin", "料=liao", "清=qing", "楚=chu",
                "说=shuo", "明=ming", "白=bai", "话=hua", "语=yu", "音=yin",
                "字=zi", "母=mu", "词=ci", "句=ju", "段=duan", "章=zhang",
                "节=jie", "页=ye", "行=hang", "列=lie", "标=biao", "点=dian",
                "号=hao", "符=fu", "号=hao", "格=ge", "式=shi", "格=ge", "局=ju",
                "空= kong", "缩=suo", "进=jin", "退=tui", "左=zuo", "右=you",
                "上=shang", "下= xia", "前=qian", "后=hou", "左=zuo", "右=you",
                "高=gao", "低=di", "长=chang", "短=duan", "宽=kuan", "窄=zhai",
                "厚=hou", "薄=bao", "深=shen", "浅=qian", "远=yuan", "近=jin",
                "快=kuai", "慢=man", "早=zao", "晚=wan", "冷=leng", "热=re",
                "难=nan", "易=yi", "新=xin", "旧=jiu", "好=hao", "坏=huai",
                "多=duo", "少=shao", "大=da", "小=xiao", "粗=cu", "细=xi",
                "软=ruan", "硬=ying", "轻=qing", "重=zhong", "黑=hei", "白=bai",
                "红=hong", "黄=huang", "蓝=lan", "绿=lv", "紫=zi", "黑=hei",
                "白=bai", "灰=hui", "明=ming", "暗=an", "清=qing", "浊= zhuo",
                "干=gan", "湿=shi", "冷=leng", "热=re", "暖=nuan", "凉=liang",
                "酸=suan", "甜=tian", "苦=ku", "辣=la", "咸=xian", "淡=dan",
                "香=xiang", "臭=chou", "脏=zang", "净=jing", "美=mei", "丑=chou",
                "真=zhen", "假=jia", "实=shi", "虚=xu", "是=shi", "非=fei",
                "有=you", "无=wu", "是=shi", "否=fou", "可=ke", "否=fou",
                "能=neng", "会=hui", "要=yao", "想=xiang", "可=ke", "以=yi",
                "中=zhong", "间=jian", "内=nei", "外=wai", "前=qian", "后=hou",
                "上=shang", "下= xia", "里=li", "外=wai", "东=dong", "西=xi",
                "南=nan", "北=bei", "左=zuo", "右=you", "上=shang", "下= xia",
                "高=gao", "低=di", "深=shen", "浅=qian", "远=yuan", "近=jin",
                "快=kuai", "慢=man", "早=zao", "晚=wan", "先=xian", "后=hou",
                "始=shi", "终=zhong", "过=guo", "现=xian", "在=zai", "将=jiang",
                "要=yao", "能=neng", "够=gou", "会=hui", "可=ke", "以=yi",
                "应=ying", "该=gai", "当=dang", "必=bi", "须=xu", "需=xu",
                "要=yao", "想=xiang", "愿=yuan", "意=yi", "希=xi", "望=wang",
                "期=qi", "待=dai", "等=deng", "候=hou", "欢=huan", "迎=ying",
                "接=jie", "待=dai", "欢=huan", "送=song", "告=gao", "诉=su",
                "请=qing", "求=qiu", "问=wen", "答=da", "询=xun", "查=cha",
                "检=jian", "测=ce", "调=diao", "研=yan", "究=jiu", "探=tan",
                "索=suo", "寻=xun", "求=qiu", "思=si", "考=kao", "虑=lv",
                "计=ji", "划=hua", "筹=chou", "备=bei", "规=gui", "划=hua",
                "组=zu", "织=zhi", "编=bian", "写=xie", "制=zhi", "作=zuo",
                "完=wan", "成=cheng", "建=jie", "设=s", "开=kai", "发=fa",
                "创=chuang", "新=xin", "改=gai", "进=jin", "变=bian", "化=hua",
                "革=ge", "维=wei", "护=hu", "保=bao", "养=yang", "修=xiu",
                "复=fu", "治=zhi", "疗=liao", "救=jiu", "助=zhu", "援=yuan",
                "帮=bang", "扶=fu", "持=chi", "支=zhi", "撑=cheng", "加=jia",
                "减=jian", "乘=cheng", "除=chu", "等=deng", "于=yu", "加=jia",
                "强=qiang", "弱=ruo", "增=zeng", "降=jiang", "升=sheng", "降=jiang",
                "升=sheng", "涨=zhang", "跌=die", "胜=sheng", "负=fu", "赢=ying",
                "亏=kui", "盈=ying", "亏=kui", "损=sun", "益=yi", "盈=ying",
                "亏=kui", "余=yu", "缺=que", "余=yu", "缺=que", "满=man", "空=kong",
                "满=man", "空=kong", "饱=bao", "饿=e", "渴=ke", "饿=e",
                "困=kun", "累=lei", "忙=mang", "闲=xian", "忙=mang", "闲=xian",
                "急=ji", "慢=man", "快=kuai", "慢=man", "快=kuai", "缓=huan",
                "紧=jin", "松=song", "紧=jin", "松=song", "轻=qing", "重=zhong",
                "重=zhong", "轻=qing", "厚=hou", "薄=bao", "厚=hou", "薄=bao",
                "粗=cu", "细=xi", "粗=cu", "细=xi", "宽=kuan", "窄=zhai",
                "宽=kuan", "窄=zhai", "长=chang", "短=duan", "长=chang", "短=duan",
                "高=gao", "矮=ai", "高=gao", "矮=ai", "胖=pang", "瘦=shou",
                "胖=pang", "瘦=shou", "美=mei", "丑=chou", "美=mei", "丑=chou",
                "香=xiang", "臭=chou", "香=xiang", "臭=chou", "好=hao", "坏=huai",
                "好=hao", "坏=huai", "对=dui", "错=cuo", "对=dui", "错=cuo",
                "真=zhen", "假=jia", "真=zhen", "假=jia", "是=shi", "非=fei",
                "是=shi", "非=fei", "有=you", "无=wu", "有=you", "无=wu",
                "在=zai", "不=bu", "在=zai", "不=bu", "来=lai", "去=qu",
                "来=lai", "去=qu", "进=jin", "出=chu", "进=jin", "出=chu",
                "开=kai", "关=guan", "开=kai", "关=guan", "始=shi", "终=zhong",
                "始=shi", "终=zhong", "分=fen", "合=he", "分=fen", "合=he",
                "离=li", "合=he", "离=li", "合=he", "聚=ju", "散=san",
                "聚=ju", "散=san", "分=fen", "配=pei", "分=fen", "配=pei",
                "组=zu", "拆=chai", "组=zu", "拆=chai", "建=jie", "拆=chai",
                "建=jie", "拆=chai", "装=zhuang", "卸=xie", "装=zhuang", "卸=xie",
                "使=shi", "用=yong", "使=shi", "用=yong", "学=xue", "习=xi",
                "学=xue", "习=xi", "教=jiao", "学=xue", "教=jiao", "学=xue",
                "研=yan", "究=jiu", "研=yan", "究=jiu", "思=si", "考=kao",
                "思=si", "考=kao", "想=xiang", "象=xiang", "想=xiang", "象=xiang",
                "理=li", "解=jie", "理=li", "解=jie", "懂=dong", "得=de",
                "懂=dong", "得=de", "知=zhi", "道=dao", "知=zhi", "道=dao",
                "觉=jue", "得=de", "觉=jue", "得=de", "感=gan", "到=dao",
                "感=gan", "到=dao", "想=xiang", "起=qi", "想=xiang", "起=qi",
                "记=ji", "住=zhu", "记=ji", "住=zhu", "忘=wang", "掉=diao",
                "忘=wang", "掉=diao", "记=ji", "忆=yi", "记=ji", "忆=yi",
                "忘=wang", "却=que", "忘=wang", "却=que", "想=xiang", "起=qi",
                "想=xiang", "起=qi", "怀=huai", "念=nian", "怀=huai", "念=nian",
                "记=ji", "得=de", "记=ji", "得=de", "忘=wang", "掉=diao"
        };

        for (String pinyin : pinyins) {
            String[] parts = pinyin.split("=");
            if (parts.length == 2) {
                char hanzi = parts[0].charAt(0);
                String pinyinValue = parts[1];
                PINYIN_MAP.put(hanzi, pinyinValue);
                FULL_PINYIN_MAP.put(hanzi, pinyinValue);
            }
        }
    }

    /**
     * 汉字转拼音（首字母）
     */
    public String toPinyin(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder pinyin = new StringBuilder();
        for (char c : text.toCharArray()) {
            String p = PINYIN_MAP.get(c);
            if (p != null) {
                pinyin.append(p.charAt(0));
            } else {
                pinyin.append(c);
            }
        }
        return pinyin.toString();
    }

    /**
     * 汉字转完整拼音
     */
    public String toFullPinyin(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder pinyin = new StringBuilder();
        for (char c : text.toCharArray()) {
            String p = FULL_PINYIN_MAP.get(c);
            if (p != null) {
                pinyin.append(p);
            } else {
                pinyin.append(c);
            }
        }
        return pinyin.toString();
    }

    /**
     * 获取汉字的首字母
     */
    public String getFirstLetter(char hanzi) {
        String p = PINYIN_MAP.get(hanzi);
        return p != null ? String.valueOf(p.charAt(0)) : String.valueOf(hanzi);
    }

    /**
     * 是否包含汉字
     */
    public boolean containsChinese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }
}
