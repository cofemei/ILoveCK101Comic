package com.mei9g.iloveck101

import groovy.util.logging.Log

import org.jsoup.Jsoup
import org.jsoup.Connection.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.security.MessageDigest

@Grapes([
	@Grab('org.jsoup:jsoup:1.6.1') 
])

@Log
class ILoveCk101Comic {
	
	// Comic -> Volume -> volpage -> img
	final String BASE_URL = "http://m.comic.ck101.com/"
//    final String url = "http://m.comic.ck101.com/comic/16687"
	def downloadFolder

    def htmlCache = [:]

	def ILoveCk101Comic() {
        // create folder in Destop
        def desktopPath = System.getProperty("user.home") + File.separator + 'Desktop'

        this.downloadFolder = new File(desktopPath, 'ILoveCk101Comic')
        if (downloadFolder.exists() == false)
            downloadFolder.mkdir()
	}

    def jsoupWapper(String url) {
        String urlMd5 = MessageDigest.getInstance("MD5").digest(url.bytes).encodeHex().toString()

        if (htmlCache.containsKey(urlMd5)) {
            return htmlCache.get(urlMd5)
        }

        if (url) {
            for (i in 1..10) {
                try {
                    Response response = Jsoup.connect(url)
                            .header('Host', 'm.comic.ck101.com')
                            .header('Connection', 'keep-alive')
                            .header('Cache-Control', 'max-age=0')
                            .header('Accept', 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8')
                            .header('Accept-Encoding', 'gzip,deflate,sdch')
                            .header('Accept-Language', 'zh-TW,zh;q=0.8,en-US;q=0.6,en;q=0.4,ja;q=0.2')
                            .userAgent('Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36')
                            .timeout(60000)
                            .execute()
                    if (response.statusCode() != 200) {
                        log.info("Status code is $response.statusCode(), retrying ...")
                        continue
                    }
                    htmlCache.put(urlMd5, response.parse())
//                    return response.parse()
                    return htmlCache.get(urlMd5)
                } catch (IOException e) {
                    e.printStackTrace()
                }
            }
        }
        reutrn null
    }

	def makeDir(String title, String vol) {
		File folder = new File(this.downloadFolder, "$title/$title - $vol")
		if (!folder.exists())
			folder.mkdirs()
		return folder
	}

	def parserComicTitle(String url) {
		def doc = this.jsoupWapper(url)
		def title = doc.select("h1[itemprop]")
		return title.text()
	}

    def parserTotle(String url) {
        def doc = this.jsoupWapper(url)
        def totlePage = doc.select(".page > ul > li")[1].text()
        if (totlePage) {
            return Integer.parseInt(totlePage.toString().split('/')[1])
        } else {
            return 1
        }
    }

	def parserVolume(String url) {
		def doc = this.jsoupWapper(url)
		def allLink = doc.select("a[href]")
		def comicList = []
		for( i in allLink ) {
			if (i =~ /vols/) {
				comicList << [ title:i.attr("title") ,url: BASE_URL + i.attr("href") ]
			}
		}
		return comicList
	}

	def parserPage(String url) {
		def doc = this.jsoupWapper(url)
		def totlePage = doc.select(".pageBox")[0].text()
		def match = totlePage =~ /([0-9]+)\/([0-9]+)/
		if( match.size() > 0){
			totlePage = match[0][2].toInteger()
		}

		def pageurl = []
		def volbase = this.getVolumeBaseUrl(url)

		for( int i=0; i <= totlePage; i++ ) {
			pageurl << volbase + "/" + i	
		}
			
		return pageurl
	}

	def parserImageUrl(String url) {
		def pagedoc = this.jsoupWapper(url)
		def link = pagedoc.select("img[src]")
		for ( i in link ) {
			if (i =~ /onerror/) {
				return i.attr("src")
			}
		}
	}	

	def getVolumeBaseUrl(String url) {
		url.substring(0, url.lastIndexOf("/"))
	}

    def featch(def url) {
        def comicTitle = this.parserComicTitle(url)

        def totlePage = parserTotle(url)
        (1 .. totlePage).each { pageNb ->
            def vols = parserVolume(url + "/0/${pageNb}")
            for (p in vols) {
                def pageTitle = p.title
                def pageUrl = p.url
                def imageUrl = this.parserPage(pageUrl)

                for (int i = 0; i <= imageUrl.size()-1; i++) {
                    def folder = makeDir(comicTitle, pageTitle)
                    def image = this.parserImageUrl(imageUrl[i])
                    if (image != null && image.length() != 0) {
                        File downloadFile = new File(folder, "${i.toString().padLeft(3,'0')}.png")
                        println "${image} ${downloadFile.exists() ? "exists" : "download" }"
                        if ( !downloadFile.exists() ) {
                            downloadFile.withOutputStream { out ->
                                URLConnection openConnection = new URL(image).openConnection()
                                openConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0")
                                out << openConnection.getInputStream()
                            }
                        }
                    }
                }
            }
        }


    }


	static main(args) {
		def comic = new ILoveCk101Comic()
		if (args){
		    String url = args[0]
		    comic.featch(url)
		} else {
		    println "Please provide URL from ck101"
		}
	}		

}
