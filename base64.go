package main

import (
	"encoding/base64"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

const (
	base64Table = "123QRSTUabcdVWXYZHijKLAWDCABDstEFGuvwxyzGHIJklmnopqr234560178912"
)

var coder = base64.NewEncoding(base64Table)

func base64Encode(src []byte) []byte {
	// return []byte(coder.EncodeToString(src))
	return []byte(base64.StdEncoding.EncodeToString(src))
}

func base64Decode(src []byte) ([]byte, error) {
	// return coder.DecodeString(string(src))
	return base64.StdEncoding.DecodeString(string(src))
}

func main() {

	argNum := len(os.Args)
	if argNum < 2 {
		fmt.Println("usage: <cammandexec> filename")
		os.Exit(1)
	}
	var filename = os.Args[1]
	fmt.Printf("file: %s\n", filename)

	data, err := ioutil.ReadFile(filename)
	herr(err)

	mimetype := http.DetectContentType(data)
	fmt.Println(mimetype)

	outfilename := "out-" + strconv.FormatInt(time.Now().Unix(), 10)
	if strings.Contains(mimetype, "text/plain;") {
		decdata, err := base64Decode(data)
		herr(err)

		mimetype_out := http.DetectContentType(decdata)

		if strings.Contains(mimetype_out, "application/pdf") {
			outfilename = outfilename + ".pdf"
		} else if strings.Contains(mimetype_out, "image/png") {
			outfilename = outfilename + ".png"
		} else if strings.Contains(mimetype_out, "image/jpeg") {
			outfilename = outfilename + ".jpg"
		} else {
			outfilename = outfilename + ".out"
		}
		err = ioutil.WriteFile(outfilename, decdata, 0664)
		herr(err)

	} else {
		outfilename = outfilename + ".txt"
		encdata := base64Encode(data)

		err = ioutil.WriteFile(outfilename, encdata, 0664)
		herr(err)

	}
	fmt.Println(outfilename)

}

func herr(err error) {
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
