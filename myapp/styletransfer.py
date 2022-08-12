from __future__ import print_function

import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim

from PIL import Image
import matplotlib.pyplot as plt

import torchvision.transforms as transforms
import torchvision.models as models

import copy
import os
import numpy as np 
import random
import time 
import torch.nn.functional as F
from torchvision.utils import save_image

torch.backends.cudnn.deterministic = True # Use cudnn as deterministic mode for reproducibility
torch.backends.cudnn.benchmark = False


def image_loader(image_name):
    image = Image.open(image_name)
    # fake batch dimension required to fit network's input dimensions
    image = loader(image).unsqueeze(0)
    return image.to(device, torch.float)

unloader = transforms.ToPILImage()  # reconvert into PIL image
def imshow(tensor, title=None):
    image = tensor.cpu().clone()  # we clone the tensor to not do changes on it
    image = image.squeeze(0)      # remove the fake batch dimension
    image = unloader(image)
#     plt.imshow(image)
#     if title is not None:
#         plt.title(title)

# plt.figure(figsize=(12, 8))
# ax = plt.subplot(1, 2, 1)
# imshow(style_img, title='Style Image')

# plt.subplot(1, 2, 2)
# imshow(content_img, title='Content Image')
# plt.show()
#Here, you first need to split submodules in VGG-19 to construct content loss and style loss.
#Specifically, you need to store each submodule in a form of "torch.nn.modules.container.Sequential".
# The "Sequential" module is a container of nn.Module objects. With Sequential containers,
# you can stack any layer and compose them altogether. First, you should download a pre-trained VGG-19 model.


#To define style loss and content loss, After obtaining the intermediate feature of the model,
# you must calculate the Gram matrix.
# Defines a function that obtains the output value of the intermediate layer of the model.
def get_features(x, model, layers):
    features = {}
    for name, layer in enumerate(model.children()): # 0, conv
        x = layer(x)
        if str(name) in layers:
            features[layers[str(name)]] = x
    return features



def gram_matrix(feature_map):
    _, d, h, w = feature_map.size()
    # reshape so we're multiplying the features for each channel
    feature_map = feature_map.view(d, h * w)
    
    # calculate the gram matrix
    G = torch.mm(feature_map, feature_map.t())
    return G


def get_content_loss(pred_features, target_features, layer):
    target = target_features[layer]
    pred = pred_features[layer]
    loss = F.mse_loss(pred, target)
    return loss


def get_style_loss(pred_features, target_features, style_layers_dict):
    loss = 0
    for layer in style_layers_dict:
        pred_fea = pred_features[layer]
        pred_gram = gram_matrix(pred_fea)
        n, c, h, w = pred_fea.shape
        target_gram = gram_matrix(target_features[layer])
        layer_loss = style_layers_dict[layer] * F.mse_loss(pred_gram, target_gram)
        loss += layer_loss / (n*c*h*w)
    return loss




# plt.figure(figsize=(16, 8))
# plt.subplot(1, 3, 1)
# imshow(style_img, title='Style Image')
# plt.subplot(1, 3, 2)
# imshow(content_img, title='Content Image')
# plt.subplot(1, 3, 3)
# imshow(input_img, title='Output Image')
# plt.show()

def main(user_img_path,style_img_path):
    global device
    device = 'cuda' if torch.cuda.is_available() else 'cpu'

    # desired size of the output image
    imsize = 512 if torch.cuda.is_available() else 128  # use small size if no gpu

    global loader
    loader = transforms.Compose([
        transforms.Resize((imsize, imsize)),  # scale imported image
        transforms.ToTensor()])  # transform it into a torch tensor

    # style_img = image_loader("./static/style.jpg")
    # content_img = image_loader("./static/5.jpg")
    style_img = image_loader(style_img_path)
    content_img = image_loader(user_img_path)
    assert style_img.size() == content_img.size(), \
        "we need to import style and content images of the same size"
    

    cnn = models.vgg19(pretrained=True).features.to(device)

    for name, param in cnn.named_parameters():
        param.requires_grad = False

    for layer in cnn:
        if isinstance(layer, nn.ReLU):
            layer.inplace = False

        #Obtain features for content and style image.
    feature_layers = {'0': 'conv1_1',
                    '5': 'conv2_1',
                    '10': 'conv3_1',
                    '19': 'conv4_1',
                    '21': 'conv4_2',
                    '28': 'conv5_1'}


    content_features = get_features(content_img, cnn, feature_layers)
    style_features = get_features(style_img, cnn, feature_layers)

    for key in content_features.keys():
        print(content_features[key].shape)

    #Training
    #Based on the content loss  Jcontent  and style loss  Jstyle , the overall loss is computed as the following weighted sum
    #Jtotal=wc⋅Jcontent+ws⋅Jstyle 
    #where wc and ws correspond to the content weight and style weight, respectively.
    #Based on the total loss, update the generated image via gradient descent and backpropagation.
    input_img = content_img.clone()
    optimizer = optim.Adam([input_img.requires_grad_()], lr=0.01)
    content_weight = 1e1
    style_weight = 1e4
    iteration = 2000           
    content_layer = 'conv5_1'
    style_layers_dict = {'conv1_1':0.75,
                        'conv2_1':0.5,
                        'conv3_1':0.25,
                        'conv4_1':0.25,
                        'conv5_1':0.25}
                        
    for i in range(iteration):
        input_features = get_features(input_img, cnn, feature_layers) # feature_layers에 해당하는 layer의 출력값 얻기
        content_loss = get_content_loss(input_features, content_features, content_layer) # 
        style_loss = get_style_loss(input_features, style_features, style_layers_dict)
        loss = content_weight * content_loss + style_weight * style_loss

        optimizer.zero_grad()                                                      
        loss.backward()                                                            
        optimizer.step()    
        
        if i % 100 == 0:
            fname = 'style_transfer_result.jpg'
            print("run [{}]:".format(i))
            print('Style Loss : {:4f} Content Loss: {:4f}'.format(
                (style_weight * style_loss).item(), (content_weight * content_loss).item()))
            print()
            
    input_img.data.clamp_(0, 1)
    #print('train done')
    save_image(input_img,'./static/'+fname)

    return '/static/'+fname