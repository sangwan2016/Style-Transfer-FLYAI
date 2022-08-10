from flask import Flask, render_template, request
import os
#from . import neural_style_transfer

app=Flask(__name__)

@app.route("/")

def main():
    return render_template('index.html')

@app.route('/uploader', methods=['GET','POST'])
def uploader_file():
    path=os.getcwd()
    if request.method=='POST':
        user_img=request.files['user_img']
        user_img.save(path+'/static/'+str(user_img.filename))
        user_img_path=path+'/static/'+str(user_img.filename)

        style_img=request.files['style_img']
        style_img.save(path+'/static/'+str(style_img.filename))
        style_img_path=path+'/static/'+str(style_img.filename)

        #transfer_img=neural_style_transfer(user_img_path,style_img_path)
        #transfer_img_path=path+'/static/'+str(transfer_img.filename)

        return render_template('post.html', user_img=str(user_img.filename), style_img=str(style_img.filename))

if __name__=="__main__":
    app.run()