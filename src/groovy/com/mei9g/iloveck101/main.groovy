package com.mei9g.iloveck101

import groovy.util.logging.Log

import org.jsoup.Jsoup
import org.jsoup.Connection.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

@Grapes([
	@Grab('org.jsoup:jsoup:1.6.1') 
])
@Log
class ILoveCk101Comic {
	
	// Comic -> Volume -> volpage -> img
	def baseurl = "http://m.comic.ck101.com/"
	def url = "http://m.comic.ck101.com/comic/16687"
	def comicTitle = ""
	def desktopPath
	def baseFolder

	def ILoveCk101Comic() {
		if (!this.desktopPath) {
			this.desktopPath = System.getProperty("user.home") + File.separator + 'Desktop'
		}
		
		if (!this.baseFolder) {
			this.baseFolder = new File(this.desktopPath, 'ILoveCk101Comic')
			if (baseFolder.exists() == false) {
				baseFolder.mkdir()
			}
		}
	}

	def run(def url) {
		this.url = url
		this.comicTitle = this.parserComicTitle(this.url)

		def vols = parserVolumeUrl(this.url)
		for (p in vols) {
			def pageTitle = p.title
			def pageUrl = p.url
			def imageUrl = this.parserPageUrl(pageUrl)

			for (int i = 0; i <= imageUrl.size()-1; i++) {
				def folder = makeDir( this.comicTitle.toString(), pageTitle)
				def image = this.parserImageUrl(imageUrl[i])
				if (image != null && image.length() != 0) {
					new File(folder, "${i.toString().padLeft(3,'0')}.png").withOutputStream { out ->
		                    out << new URL(image).openStream()
		            }
				}
			}
		}	
	}

	def makeDir(String title,String vol) {		
		File folder = new File(this.baseFolder, "$title - $vol")
		if (folder.exists() == false) {
			folder.mkdir()
		}

		return folder
	}

	def parserComicTitle(String url) {
		def doc = this.jsoupWapper(url)
		def title = doc.select("h1[itemprop]")
		return title.text()
	}

	def parserVolumeUrl(String url) {
		def doc = this.jsoupWapper(url)
		def allLink = doc.select("a[href]")
		def comicList = []
		for( i in allLink ) {
			if (i =~ /vols/) {
				comicList << [ title:i.attr("title") ,url: baseurl + i.attr("href") ]
			}
		}
		return comicList
	}

	def parserPageUrl(String url) {
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

	def jsoupWapper(String url) {
		if (url)
		for (i in 1..10) {
			try {
				Response response = Jsoup.connect(url)
					.header('Host', 'ck101.com')
					.header('Connection', 'keep-alive')
					.header('Cache-Control', 'max-age=0')
					.header('Accept', 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8')
					.header('Accept-Encoding', 'gzip,deflate,sdch')
					.header('Accept-Language', 'zh-TW,zh;q=0.8,en-US;q=0.6,en;q=0.4,ja;q=0.2')	
					.userAgent('Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36')
					.timeout(60000)
					.execute()
				if (response.statusCode() != 200)
                {
                    log.info("Status code is $response.statusCode(), retrying ...")
                    continue
                }
                return response.parse()

			} catch (IOException e) {
				e.printStackTrace()
			}
		}
		reutrn null
	}

	def getVolumeBaseUrl(String url) {
		url.substring(0, url.lastIndexOf("/"))
	}

	static main(args) {
		def comic = new ILoveCk101Comic()
		if (args){
		    String url = args[0]
		    comic.run(url)
		} else {
		    println "Please provide URL from ck101"
		}
	}		

}
