@echo off
set IM6=C:\Program Files\ImageMagick-6.1.6-Q8

cd figures

: *** WMF-to-GIF (doesn't work properly: hairlines get lost) ***
: for %%I in (*.wmf) do echo %%I to %%~nI.gif && "%IM6%"\convert -density 300 -trim %%I %%~nI.gif

: *** trim GIFs ***
: IM6's -trim conversion sucks: geometry meta information gets screwed up.
: So we're forced to do it via BMP (which surely doesn't contain any meta tags
: at all) (use +repage option?)
for %%I in (*.gif) do echo crop %%I && "%IM6%"\convert -trim %%I %%~nI.bmp
for %%I in (*.bmp) do "%IM6%"\convert %%I %%~nI.gif
del *.bmp

: *** PDF conversion follows (commented out because rasterized PDFs look very bad) ***
: convert to png because the gifs still contain some garbage meta-tag which screws up pdf
: for %%I in (*.gif) do echo %%I to %%~nI.png && "%IM6%"\convert %%I %%~nI.png
: for %%I in (*.png) do echo %%I to %%~nI.pdf && "%IM6%"\convert %%I %%~nI.pdf
: del *.png
