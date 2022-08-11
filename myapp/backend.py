from flask import Flask, render_template, request, send_file
import os

#import neural_style_transfer
import styletransfer
from PIL import Image

app=Flask(__name__)

@app.route("/")

def main():
    return render_template('index.html')

@app.route('/uploader', methods=['GET','POST'])
def uploader_file():
    path=__file__.split('\\')
    path.pop()
    path='\\'.join(path)
    os.chdir(path)
    if request.method=='POST':
        user_img=request.files['user_img']
        user_img.save(path+'/static/'+str(user_img.filename))
        user_img_path=path+'/static/'+str(user_img.filename)

        style_img=request.files['style_img']
        style_img.save(path+'/static/'+str(style_img.filename))
        style_img_path=path+'/static/'+str(style_img.filename)

        #transfer_img=neural_style_transfer.main(user_img_path,style_img_path)
        transfer_img=styletransfer.main(user_img_path,style_img_path)
        #transfer_img.save(path+'/static/'+transfer_img)
        #transfer_img_path=path+'/static/'+str(transfer_img)
        transfer_img_path=path+str(transfer_img)


        #return render_template('post.html', user_img=str(user_img.filename), style_img=str(style_img.filename), transfer_img=transfer_img_path)
        # return render_template('post.html', transfer_img=transfer_img_path)
        return send_file("." + transfer_img, mimetype='image/jpg')

if __name__=="__main__":
    app.run(host="172.20.10.9", port=5000)